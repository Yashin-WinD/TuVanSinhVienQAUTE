package vn.ute.qaute.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ute.qaute.dto.request.ChatMessageDto;
import vn.ute.qaute.dto.response.ApiResponse;
import vn.ute.qaute.entity.ChatMessage;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.service.ChatService;
import vn.ute.qaute.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatApiController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getMessages(@PathVariable("consultationId") Long consultationId) {
        List<ChatMessage> messages = chatService.getChatHistory(consultationId);
        List<ChatMessageDto> dtos = messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Tải lịch sử tin nhắn thành công", dtos));
    }

    @PostMapping("/{consultationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("consultationId") Long consultationId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            chatService.markMessagesAsRead(consultationId, currentUser.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đã đọc"));
    }
}
