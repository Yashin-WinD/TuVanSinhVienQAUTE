package vn.ute.qaute.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.service.CloudinaryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public CloudinaryResponseDto uploadImage(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String folder = (folderName != null && !folderName.isEmpty()) ? "qaute/" + folderName : "qaute/general";
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            ));

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String format = (String) uploadResult.get("format");
            Long bytes = ((Number) uploadResult.get("bytes")).longValue();
            String resourceType = (String) uploadResult.get("resource_type");

            logger.info("Tải file lên Cloudinary thành công: {}", secureUrl);
            return new CloudinaryResponseDto(publicId, secureUrl, format, bytes, resourceType);
        } catch (Exception e) {
            logger.warn("Không thể tải lên Cloudinary ({}). Hệ thống tự động chuyển sang lưu trữ cục bộ (Local Storage).", e.getMessage());
            return saveLocally(file, folderName);
        }
    }

    @Override
    public CloudinaryResponseDto uploadFile(MultipartFile file, String folderName) {
        return uploadImage(file, folderName);
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
