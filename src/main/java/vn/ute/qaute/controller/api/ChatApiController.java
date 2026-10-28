package vn.ute.qaute.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.request.ChatMessageDto;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.entity.ChatMessage;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.RoleType;
import vn.ute.qaute.service.ChatService;
import vn.ute.qaute.service.ConsultationService;
import vn.ute.qaute.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ConsultationService consultationService;
    private final UserService userService;

    @Autowired
    public ChatApiController(ChatService chatService,
                             ConsultationService consultationService,
                             UserService userService) {
        this.chatService = chatService;
        this.consultationService = consultationService;
        this.userService = userService;
    }

    private boolean canAccessConsultation(User user, Long consultationId) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == RoleType.ROLE_ADMIN || user.getRole() == RoleType.ROLE_LECTURER) {
            return true;
        }
        ConsultationRequest req = consultationService.getRequestById(consultationId);
        return req.getStudent() != null && req.getStudent().getId().equals(user.getId());
    }

    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getMessages(@PathVariable("consultationId") Long consultationId) {
        User currentUser = userService.getCurrentUser();
        if (!canAccessConsultation(currentUser, consultationId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có quyền xem lịch sử trò chuyện này"));
        }
        List<ChatMessage> messages = chatService.getChatHistory(consultationId);
        List<ChatMessageDto> dtos = messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Tải lịch sử tin nhắn thành công", dtos));
    }

    @PostMapping("/{consultationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("consultationId") Long consultationId) {
        User currentUser = userService.getCurrentUser();
        if (!canAccessConsultation(currentUser, consultationId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có quyền thao tác trên cuộc trò chuyện này"));
        }
        chatService.markMessagesAsRead(consultationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đã đọc"));
    }
}
