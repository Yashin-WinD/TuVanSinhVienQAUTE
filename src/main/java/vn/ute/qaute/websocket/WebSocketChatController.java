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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

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
        try {
            Long senderId = messageDto.getSenderId();
            String senderName = messageDto.getSenderName();
            String senderAvatar = messageDto.getSenderAvatar();

            // Lấy thông tin người gửi từ Principal nếu có, hoặc từ senderId
            User sender = null;
            if (principal != null && principal.getName() != null) {
                sender = userRepository.findByUsername(principal.getName()).orElse(null);
            }
            if (sender == null && senderId != null) {
                sender = userRepository.findById(senderId).orElse(null);
            }

            if (sender != null) {
                senderId = sender.getId();
                if (senderName == null || senderName.isBlank()) {
                    senderName = sender.getFullName();
                }
                if (senderAvatar == null || senderAvatar.isBlank()) {
                    senderAvatar = sender.getAvatarUrl();
                }
                if (sender.getRole() != null) {
                    messageDto.setSenderRole(sender.getRole().name());
                }
            }

            if (senderId == null || messageDto.getConsultationId() == null) {
                logger.warn("Không thể gửi tin nhắn chat: senderId={}, consultationId={}", senderId, messageDto.getConsultationId());
                return;
            }

            // Lưu vào cơ sở dữ liệu
            ChatMessage saved = chatService.saveMessage(messageDto, senderId);

            messageDto.setId(saved.getId());
            messageDto.setSenderId(senderId);
            messageDto.setSenderName(senderName);
            messageDto.setSenderAvatar(senderAvatar);
            messageDto.setSentAt(saved.getSentAt() != null ? saved.getSentAt() : LocalDateTime.now());

            logger.info("Chat message sent to consultation #{}: sender='{}' (id={})",
                    messageDto.getConsultationId(), senderName, senderId);

            // Phát tin nhắn tới tất cả người đang theo dõi phòng chat tư vấn này
            messagingTemplate.convertAndSend(
                    "/topic/consultation." + messageDto.getConsultationId(),
                    messageDto
            );
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý gửi tin nhắn chat realtime: {}", e.getMessage(), e);
        }
    }
}
