package vn.ute.qaute.service;

import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;

public interface CloudinaryService {
    CloudinaryResponseDto uploadImage(MultipartFile file, String folderName);
    CloudinaryResponseDto uploadFile(MultipartFile file, String folderName);
    void deleteFile(String publicId);
}
