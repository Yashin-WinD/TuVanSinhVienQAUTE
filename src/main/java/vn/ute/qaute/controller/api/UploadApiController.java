package vn.ute.qaute.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.dto.response.CloudinaryResponseDto;
import vn.ute.qaute.service.CloudinaryService;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadApiController {

    private final CloudinaryService cloudinaryService;

    @Autowired
    public UploadApiController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<CloudinaryResponseDto>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "evidence") String folder) {
        CloudinaryResponseDto responseDto = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.ok("Tải ảnh lên thành công", responseDto));
    }
}
