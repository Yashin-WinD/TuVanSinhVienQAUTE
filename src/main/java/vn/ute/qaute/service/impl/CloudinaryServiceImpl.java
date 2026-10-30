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

    // Giới hạn tối đa của gói Cloudinary Free là 10MB (10,485,760 bytes)
    private static final long CLOUDINARY_FREE_MAX_BYTES = 10_485_760L;

    private final Cloudinary cloudinary;

    @Value("${cloudinary.enabled:true}")
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

    private String extractExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String rawExt = originalFilename.substring(originalFilename.lastIndexOf("."))
                    .replaceAll("[^a-zA-Z0-9.]", "");
            return rawExt.isEmpty() ? ".bin" : rawExt.toLowerCase();
        }
        return ".png";
    }

    private CloudinaryResponseDto uploadInternal(MultipartFile file, String folderName, String defaultResourceType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String safeFolder = sanitizeFolderName(folderName);
        String extension = extractExtension(file.getOriginalFilename());

        // 1. Luôn lưu trước một bản локаl vào /uploads/{safeFolder}/ để đảm bảo tốc độ và phục vụ Xem trước (Preview)
        Path localMirrorPath = saveLocalFile(file, safeFolder, UUID.randomUUID() + extension);

        // 2. Kiểm tra điều kiện đẩy lên Cloudinary
        boolean isConfigured = cloudinaryEnabled
                && cloudName != null
                && !cloudName.trim().isEmpty()
                && !cloudName.equalsIgnoreCase("test1")
                && !cloudName.equalsIgnoreCase("test-cloud")
                && !cloudName.startsWith("test");

        if (!isConfigured) {
            return buildLocalResponse(localMirrorPath, safeFolder, extension, file.getSize());
        }

        // Gói Cloudinary Free giới hạn cứng 10MB (10,485,760 bytes) cho mỗi file.
        // Nếu file > 10MB, bỏ qua gọi mạng Cloudinary để không phải chờ upload rồi bị báo lỗi 400.
        if (file.getSize() > CLOUDINARY_FREE_MAX_BYTES) {
            logger.warn("Tệp '{}' ({} bytes) vượt quá giới hạn 10MB của gói Cloudinary Free (tối đa 10,485,760 bytes). " +
                    "Sử dụng trực tiếp bản lưu cục bộ tốc độ cao tại /uploads/{}/{}",
                    file.getOriginalFilename(), file.getSize(), safeFolder, localMirrorPath.getFileName());
            return buildLocalResponse(localMirrorPath, safeFolder, extension, file.getSize());
        }

        // 3. Đẩy trực tiếp file đã lưu từ ổ cứng lên Cloudinary (1 lần gọi duy nhất, không dùng filename_override gây lỗi ký tự tiếng Việt)
        Map<String, Object> params = new HashMap<>();
        params.put("resource_type", defaultResourceType != null ? defaultResourceType : "auto");
        params.put("folder", "qaute/" + safeFolder);

        try {
            logger.info("Đang tải tệp '{}' ({} bytes) lên Cloudinary (folder: qaute/{})...",
                    file.getOriginalFilename(), file.getSize(), safeFolder);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(localMirrorPath.toFile(), params);

            if (uploadResult != null && uploadResult.get("secure_url") != null) {
                String publicId = (String) uploadResult.get("public_id");
                String secureUrl = (String) uploadResult.get("secure_url");
                String format = (String) uploadResult.get("format");
                Long bytes = uploadResult.get("bytes") != null
                        ? ((Number) uploadResult.get("bytes")).longValue()
                        : file.getSize();
                String resourceType = (String) uploadResult.get("resource_type");

                // Đổi tên bản lưu cục bộ khớp với tên file trên URL Cloudinary để trình Xem trước (Preview)
                // có thể đọc từ ổ cứng ngay lập tức mà không bị lỗi 401 PDF của Cloudinary Free
                syncLocalMirrorFileName(localMirrorPath, secureUrl);

                logger.info(">>> TẢI FILE LÊN CLOUDINARY THÀNH CÔNG: {}", secureUrl);
                return new CloudinaryResponseDto(publicId, secureUrl, format, bytes, resourceType);
            }
        } catch (Exception ex) {
            logger.warn("Không thể tải lên Cloudinary ({}), sử dụng bản lưu cục bộ /uploads/{}/{}",
                    ex.getMessage(), safeFolder, localMirrorPath.getFileName());
        }

        return buildLocalResponse(localMirrorPath, safeFolder, extension, file.getSize());
    }

    private Path saveLocalFile(MultipartFile file, String subFolder, String fileName) {
        try {
            Path baseUploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
            Path uploadPath = baseUploadRoot.resolve(subFolder).normalize();
            if (!uploadPath.startsWith(baseUploadRoot)) {
                throw new SecurityException("Đường dẫn thư mục tải lên không hợp lệ!");
            }
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetLocation = uploadPath.resolve(fileName).normalize();
            if (!targetLocation.startsWith(uploadPath)) {
                throw new SecurityException("Tên tệp tải lên không hợp lệ!");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetLocation;
        } catch (IOException ex) {
            logger.error("Lỗi khi lưu trữ file cục bộ: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể lưu trữ tệp tải lên: " + ex.getMessage(), ex);
        }
    }

    private void syncLocalMirrorFileName(Path localMirrorPath, String secureUrl) {
        try {
            int lastSlash = secureUrl.lastIndexOf('/');
            if (lastSlash > 0 && lastSlash < secureUrl.length() - 1) {
                String cldFileName = secureUrl.substring(lastSlash + 1).replaceAll("[^a-zA-Z0-9._-]", "");
                if (!cldFileName.isEmpty()) {
                    Path syncedPath = localMirrorPath.getParent().resolve(cldFileName).normalize();
                    if (syncedPath.startsWith(localMirrorPath.getParent())) {
                        Files.move(localMirrorPath, syncedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private CloudinaryResponseDto buildLocalResponse(Path localPath, String subFolder, String extension, long size) {
        String fileName = localPath.getFileName().toString();
        String fileUrl = "/uploads/" + subFolder + "/" + fileName;
        String extNoDot = extension.replace(".", "");
        String resourceType = (extNoDot.matches("(?i)jpg|jpeg|png|gif|webp|bmp|svg")) ? "image" : "raw";
        logger.info("Đã lưu tệp tải lên cục bộ tại: {}", fileUrl);
        return new CloudinaryResponseDto(
                "local_" + subFolder + "_" + fileName,
                fileUrl,
                extNoDot,
                size,
                resourceType
        );
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
