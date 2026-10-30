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
import java.util.concurrent.CompletableFuture;

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
        return uploadInternal(file, folderName, "auto");
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

        long startTime = System.currentTimeMillis();
        String safeFolder = sanitizeFolderName(folderName);
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        long fileSize = file.getSize();
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + extension;

        // 1. Lưu trực tiếp xuống ổ cứng cục bộ (/uploads/{safeFolder}/) chỉ trong vài mili-giây
        Path localMirrorPath = saveLocalFile(file, safeFolder, storedFileName);
        CloudinaryResponseDto response = buildLocalResponse(localMirrorPath, safeFolder, extension, fileSize);
        long localElapsed = System.currentTimeMillis() - startTime;
        logger.info("Đã lưu tức thì tệp '{}' vào {} ({} bytes trong {} ms)",
                originalFilename, response.getUrl(), fileSize, localElapsed);

        // 2. Đồng bộ ngầm (Async Non-Blocking) lên Cloudinary để giao diện người dùng phản hồi tức thì (< 0.05s)
        // mà Cloudinary Dashboard (qaute/evidence, qaute/documents, qaute/answers, qaute/avatars...) vẫn cập nhật đầy đủ
        boolean isConfigured = cloudinaryEnabled
                && cloudName != null
                && !cloudName.trim().isEmpty()
                && !cloudName.equalsIgnoreCase("test1")
                && !cloudName.equalsIgnoreCase("test-cloud")
                && !cloudName.startsWith("test");

        if (isConfigured) {
            if (fileSize <= CLOUDINARY_FREE_MAX_BYTES) {
                final String resourceTypeParam = defaultResourceType != null ? defaultResourceType : "auto";
                CompletableFuture.runAsync(() -> syncToCloudinaryAsync(
                        localMirrorPath, safeFolder, uuid, originalFilename, fileSize, resourceTypeParam));
            } else {
                logger.info("Tệp '{}' ({} bytes) > 10MB (vượt giới hạn gói Cloudinary Free), giữ bản lưu cục bộ tốc độ cao.",
                        originalFilename, fileSize);
            }
        }

        return response;
    }

    private void syncToCloudinaryAsync(Path localFile, String safeFolder, String publicIdName,
                                       String originalFilename, long fileSize, String resourceTypeParam) {
        try {
            if (!Files.exists(localFile)) {
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("resource_type", resourceTypeParam);
            params.put("folder", "qaute/" + safeFolder);
            params.put("public_id", publicIdName);
            params.put("overwrite", true);

            logger.info("[Async Cloudinary] Đang đồng bộ ngầm '{}' ({} bytes) lên Cloudinary (qaute/{})...",
                    originalFilename, fileSize, safeFolder);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(localFile.toFile(), params);
            if (uploadResult != null && uploadResult.get("secure_url") != null) {
                logger.info(">>> [Async Cloudinary] ĐỒNG BỘ THÀNH CÔNG LÊN CLOUDINARY: {}",
                        uploadResult.get("secure_url"));
            }
        } catch (Exception ex) {
            logger.warn("[Async Cloudinary] Không thể đồng bộ '{}' lên Cloudinary ({}), tệp vẫn an toàn tại {}",
                    originalFilename, ex.getMessage(), localFile);
        }
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

            try {
                file.transferTo(targetLocation);
            } catch (Exception transferEx) {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return targetLocation;
        } catch (IOException ex) {
            logger.error("Lỗi khi lưu trữ file cục bộ: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể lưu trữ tệp tải lên: " + ex.getMessage(), ex);
        }
    }

    private CloudinaryResponseDto buildLocalResponse(Path localPath, String subFolder, String extension, long size) {
        String fileName = localPath.getFileName().toString();
        String fileUrl = "/uploads/" + subFolder + "/" + fileName;
        String extNoDot = extension.replace(".", "");
        String resourceType = (extNoDot.matches("(?i)jpg|jpeg|png|gif|webp|bmp|svg")) ? "image" : "raw";
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

                    // Đồng thời xóa ngầm trên Cloudinary nếu có
                    String fileName = path.getFileName().toString();
                    int dotIdx = fileName.lastIndexOf('.');
                    String uuid = (dotIdx > 0) ? fileName.substring(0, dotIdx) : fileName;
                    String folder = path.getParent().getFileName().toString();
                    String cldPublicId = "qaute/" + folder + "/" + uuid;
                    CompletableFuture.runAsync(() -> {
                        try {
                            cloudinary.uploader().destroy(cldPublicId, ObjectUtils.emptyMap());
                        } catch (Exception ignored) {
                        }
                    });
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

        CompletableFuture.runAsync(() -> {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (IOException e) {
                logger.error("Lỗi khi xóa file trên Cloudinary: {}", e.getMessage());
            }
        });
    }
}
