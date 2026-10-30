package vn.ute.qaute.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.entity.Notification;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.service.NotificationService;
import vn.ute.qaute.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationApiController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.ok("Chưa đăng nhập", List.of()));
        }
        List<Notification> list = notificationService.getNotificationsForUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách thông báo thành công", list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("id") Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            notificationService.markAsRead(id, currentUser.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đã đọc"));
    }
}
