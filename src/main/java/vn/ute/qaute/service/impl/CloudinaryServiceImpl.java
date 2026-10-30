package vn.ute.qaute.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.service.CloudinaryService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    private final Cloudinary cloudinary;

    @Value("${cloudinary.enabled:false}")
    private boolean cloudinaryEnabled;

    @Value("${cloudinary.upload-preset:test1file}")
    private String uploadPreset;

    @Value("${cloudinary.cloud-name:test1}")
    private String cloudName;

    @Autowired
    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public CloudinaryResponseDto uploadImage(MultipartFile file, String folderName) {
        return uploadInternal(file, folderName, "image");
    }

    @Override
    public CloudinaryResponseDto uploadFile(MultipartFile file, String folderName) {
        return uploadInternal(file, folderName, "auto");
    }

    private String sanitizeFolderName(String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) {
            return "general";
        }
        String cleaned = folderName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isEmpty() ? "general" : cleaned;
    }

    private CloudinaryResponseDto uploadInternal(MultipartFile file, String folderName, String defaultResourceType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String safeFolder = sanitizeFolderName(folderName);

        // Lưu trực tiếp vào ổ cứng cục bộ đối với kho tài liệu (documents) để tốc độ tải lên tức thì
        // và đảm bảo xem trước PDF trực tiếp trong trình duyệt không bị Cloudinary chặn.
        if (!cloudinaryEnabled
                || "documents".equalsIgnoreCase(safeFolder)
                || cloudName == null
                || cloudName.trim().isEmpty()
                || cloudName.equalsIgnoreCase("test1")
                || cloudName.equalsIgnoreCase("test-cloud")
                || cloudName.startsWith("test")) {
            return saveLocally(file, safeFolder);
        }

        String originalFilename = file.getOriginalFilename();

        Map<String, Object> params = new HashMap<>();
        params.put("resource_type", defaultResourceType != null ? defaultResourceType : "auto");
        params.put("folder", "qaute/" + safeFolder);
        params.put("use_filename", true);
        params.put("unique_filename", true);
        if (originalFilename != null) {
            params.put("filename_override", originalFilename);
        }

        Path tempFile = null;
        try {
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".tmp";
            tempFile = Files.createTempFile("cld_upload_", suffix);
            file.transferTo(tempFile);

            Map<?, ?> uploadResult = cloudinary.uploader().uploadLarge(tempFile.toFile(), params);
            if (uploadResult != null) {
                String publicId = (String) uploadResult.get("public_id");
                String secureUrl = (String) uploadResult.get("secure_url");
                String format = (String) uploadResult.get("format");
                Long bytes = uploadResult.get("bytes") != null
                        ? ((Number) uploadResult.get("bytes")).longValue()
                        : file.getSize();
                String resourceType = (String) uploadResult.get("resource_type");

                logger.info(">>> TẢI FILE LÊN CLOUDINARY THÀNH CÔNG: {}", secureUrl);
                return new CloudinaryResponseDto(publicId, secureUrl, format, bytes, resourceType);
            }
        } catch (Exception ex) {
            logger.warn("Không thể tải lên Cloudinary ({}), tự động chuyển sang lưu cục bộ /uploads/{}/",
                    ex.getMessage(), safeFolder);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }

        return saveLocally(file, safeFolder);
    }

    private CloudinaryResponseDto saveLocally(MultipartFile file, String folderName) {
        try {
            long startTime = System.currentTimeMillis();
            String subFolder = sanitizeFolderName(folderName);
            Path baseUploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
            Path uploadPath = baseUploadRoot.resolve(subFolder).normalize();
            if (!uploadPath.startsWith(baseUploadRoot)) {
                throw new SecurityException("Đường dẫn thư mục tải lên không hợp lệ!");
            }
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                String rawExt = originalFilename.substring(originalFilename.lastIndexOf("."))
                        .replaceAll("[^a-zA-Z0-9.]", "");
                extension = rawExt.isEmpty() ? ".bin" : rawExt.toLowerCase();
            } else {
                extension = ".png";
            }

            String fileName = UUID.randomUUID() + extension;
            Path targetLocation = uploadPath.resolve(fileName).normalize();
            if (!targetLocation.startsWith(uploadPath)) {
                throw new SecurityException("Tên tệp tải lên không hợp lệ!");
            }

            // Sử dụng transferTo trực tiếp để đạt tốc độ ghi đĩa nhanh nhất cho file dung lượng lớn
            try {
                file.transferTo(targetLocation);
            } catch (Exception transferEx) {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            String fileUrl = "/uploads/" + subFolder + "/" + fileName;
            String extNoDot = extension.replace(".", "");
            String resourceType = (extNoDot.matches("(?i)jpg|jpeg|png|gif|webp|bmp|svg")) ? "image" : "raw";
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Đã lưu tệp tải lên cục bộ tại: {} ({} bytes trong {} ms)", fileUrl, file.getSize(), elapsed);
            return new CloudinaryResponseDto(
                    "local_" + subFolder + "_" + fileName,
                    fileUrl,
                    extNoDot,
                    file.getSize(),
                    resourceType
            );
        } catch (IOException ex) {
            logger.error("Lỗi khi lưu trữ file cục bộ: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể lưu trữ tệp tải lên: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isEmpty() || publicId.startsWith("mock_")) {
            return;
        }
        Path baseUploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        if (publicId.startsWith("/uploads/")) {
            try {
                String relative = publicId.substring("/uploads/".length());
                Path path = baseUploadRoot.resolve(relative).normalize();
                if (path.startsWith(baseUploadRoot)) {
                    Files.deleteIfExists(path);
                    logger.info("Đã xóa file cục bộ theo URL: {}", path);
                }
            } catch (IOException e) {
                logger.error("Lỗi khi xóa file cục bộ theo URL: {}", e.getMessage());
            }
            return;
        }
        if (publicId.startsWith("local_")) {
            try {
                String rest = publicId.substring("local_".length());
                int underscoreIdx = rest.lastIndexOf('_');
                if (underscoreIdx > 0) {
                    String subFolder = sanitizeFolderName(rest.substring(0, underscoreIdx));
                    String fileName = rest.substring(underscoreIdx + 1);
                    Path path = baseUploadRoot.resolve(subFolder).resolve(fileName).normalize();
                    if (path.startsWith(baseUploadRoot)) {
                        Files.deleteIfExists(path);
                        logger.info("Đã xóa file cục bộ: {}", path);
                    }
                }
            } catch (IOException e) {
                logger.error("Lỗi khi xóa file cục bộ: {}", e.getMessage());
            }
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            logger.error("Lỗi khi xóa file trên Cloudinary: {}", e.getMessage());
        }
    }
}
