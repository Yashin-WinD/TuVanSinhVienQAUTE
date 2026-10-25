package vn.ute.qaute.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import vn.ute.qaute.dto.request.ChatMessageDto;
import vn.ute.qaute.entity.ChatMessage;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.repository.UserRepository;
import vn.ute.qaute.service.ChatService;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class WebSocketChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketChatController(ChatService chatService,
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto messageDto, Principal principal) {
        Long senderId = messageDto.getSenderId();
        String senderName = messageDto.getSenderName();
        String senderAvatar = messageDto.getSenderAvatar();

        // Lấy thông tin người gửi từ Principal nếu có, hoặc từ senderId
        if (principal != null) {
            User sender = userRepository.findByUsername(principal.getName()).orElse(null);
            if (sender != null) {
                senderId = sender.getId();
                senderName = sender.getFullName();
                senderAvatar = sender.getAvatarUrl();
                messageDto.setSenderRole(sender.getRole().name());
            }
        } else if (senderId != null) {
            User sender = userRepository.findById(senderId).orElse(null);
            if (sender != null) {
                if (senderName == null) senderName = sender.getFullName();
                if (senderAvatar == null) senderAvatar = sender.getAvatarUrl();
                if (messageDto.getSenderRole() == null && sender.getRole() != null) {
                    messageDto.setSenderRole(sender.getRole().name());
                }
            }
        }

        // Lưu vào cơ sở dữ liệu
        ChatMessage saved = chatService.saveMessage(messageDto, senderId);

        messageDto.setSenderId(senderId);
        messageDto.setSenderName(senderName);
        messageDto.setSenderAvatar(senderAvatar);
        messageDto.setSentAt(saved.getSentAt() != null ? saved.getSentAt() : LocalDateTime.now());

        // Phát tin nhắn tới tất cả người đang theo dõi phòng chat tư vấn này
        messagingTemplate.convertAndSend(
                "/topic/consultation." + messageDto.getConsultationId(),
                messageDto
        );
    }
}
