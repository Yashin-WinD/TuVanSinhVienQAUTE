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

    private CloudinaryResponseDto uploadInternal(MultipartFile file, String folderName, String defaultResourceType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Nếu thông tin Cloudinary là mẫu hoặc chưa cấu hình hợp lệ, lưu trực tiếp vào server cục bộ
        if (cloudName == null || cloudName.trim().isEmpty() 
                || cloudName.equalsIgnoreCase("test1") 
                || cloudName.equalsIgnoreCase("test-cloud")
                || cloudName.startsWith("test")) {
            logger.info("Đang ở chế độ lưu trữ tệp cục bộ (Cloudinary: {}). Thư mục: /uploads/{}/", cloudName, folderName);
            return saveLocally(file, folderName);
        }

        String originalFilename = file.getOriginalFilename();
        String subFolder = (folderName != null && !folderName.trim().isEmpty()) ? folderName.trim() : "general";

        Map<String, Object> params = new HashMap<>();
        params.put("resource_type", defaultResourceType != null ? defaultResourceType : "auto");
        params.put("folder", "qaute/" + subFolder);
        params.put("use_filename", true);
        params.put("unique_filename", true);
        if (originalFilename != null) {
            params.put("filename_override", originalFilename);
        }

        Map<?, ?> uploadResult = null;
        Exception lastException = null;
        Path tempFile = null;

        try {
            // Tạo tệp tạm thời để upload (hỗ trợ uploadLarge cho cả file lớn > 10MB)
            String prefix = "cld_upload_";
            String suffix = (originalFilename != null && originalFilename.contains(".")) 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".tmp";
            tempFile = Files.createTempFile(prefix, suffix);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 1. Thử Signed Upload chuẩn (Dùng API Key & API Secret, KHÔNG nhét unsigned upload_preset vào)
            try {
                logger.info("Đang tải tệp '{}' lên Cloudinary (Signed uploadLarge, resource_type: {})...", originalFilename, params.get("resource_type"));
                uploadResult = cloudinary.uploader().uploadLarge(tempFile.toFile(), params);
            } catch (Exception e1) {
                lastException = e1;
                logger.warn("Signed Upload thất bại ({}), đang thử Signed Upload thông thường...", e1.getMessage());

                try {
                    uploadResult = cloudinary.uploader().upload(tempFile.toFile(), params);
                    lastException = null;
                } catch (Exception e2) {
                    lastException = e2;
                    logger.warn("Signed Upload thông thường thất bại ({}), đang thử Unsigned Upload...", e2.getMessage());

                    // 2. Nếu Signed thất bại và có upload_preset, thử Unsigned Upload
                    if (uploadPreset != null && !uploadPreset.trim().isEmpty()) {
                        try {
                            Map<String, Object> unsignedParams = new HashMap<>();
                            unsignedParams.put("resource_type", defaultResourceType != null ? defaultResourceType : "auto");
                            uploadResult = cloudinary.uploader().unsignedUpload(tempFile.toFile(), uploadPreset.trim(), unsignedParams);
                            lastException = null;
                        } catch (Exception e3) {
                            lastException = e3;
                            logger.warn("Unsigned Upload thất bại: {}", e3.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            lastException = ex;
            logger.error("Lỗi chuẩn bị tệp upload: {}", ex.getMessage());
        } finally {
            // Xóa file tạm thời sau khi xử lý xong
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }

        if (uploadResult != null) {
            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String format = (String) uploadResult.get("format");
            Long bytes = uploadResult.get("bytes") != null ? ((Number) uploadResult.get("bytes")).longValue() : file.getSize();
            String resourceType = (String) uploadResult.get("resource_type");

            logger.info(">>> TẢI FILE LÊN CLOUDINARY THÀNH CÔNG: {}", secureUrl);
            return new CloudinaryResponseDto(publicId, secureUrl, format, bytes, resourceType);
        } else {
            String errorMsg = lastException != null ? lastException.getMessage() : "Unknown Cloudinary error";
            logger.warn("====================================================================");
            logger.warn("KHÔNG THỂ TẢI LÊN CLOUDINARY: {}", errorMsg);
            if (lastException != null) {
                logger.warn("Chi tiết lỗi Cloudinary:", lastException);
            }
            logger.warn("-> Hệ thống tự động chuyển sang lưu trữ cục bộ: /uploads/{}/", folderName);
            logger.warn("====================================================================");
            return saveLocally(file, folderName);
        }
    }

    private CloudinaryResponseDto saveLocally(MultipartFile file, String folderName) {
        try {
            String subFolder = (folderName != null && !folderName.trim().isEmpty()) ? folderName.trim() : "general";
            Path uploadPath = Paths.get("uploads", subFolder).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".png";
            }

            String fileName = UUID.randomUUID() + extension;
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + subFolder + "/" + fileName;
            logger.info("Đã lưu tệp tải lên cục bộ tại: {}", fileUrl);
            return new CloudinaryResponseDto("local_" + subFolder + "_" + fileName, fileUrl, extension.replace(".", ""), file.getSize(), "image");
        } catch (IOException ex) {
            logger.error("Lỗi khi lưu trữ file cục bộ: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể lưu trữ tệp ảnh tải lên: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isEmpty() || publicId.startsWith("mock_")) {
            return;
        }
        if (publicId.startsWith("local_")) {
            try {
                String rest = publicId.substring("local_".length());
                int underscoreIdx = rest.indexOf('_');
                if (underscoreIdx > 0) {
                    String subFolder = rest.substring(0, underscoreIdx);
                    String fileName = rest.substring(underscoreIdx + 1);
                    Path path = Paths.get("uploads", subFolder, fileName).toAbsolutePath().normalize();
                    Files.deleteIfExists(path);
                    logger.info("Đã xóa file cục bộ: {}", path);
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
