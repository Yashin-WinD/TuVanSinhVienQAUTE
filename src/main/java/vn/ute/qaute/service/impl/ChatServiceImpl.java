package vn.ute.qaute.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ute.qaute.dto.request.ChatMessageDto;
import vn.ute.qaute.entity.ChatMessage;
import vn.ute.qaute.entity.ConsultationRequest;
import vn.ute.qaute.entity.User;
import vn.ute.qaute.entity.enums.MessageType;
import vn.ute.qaute.exception.ResourceNotFoundException;
import vn.ute.qaute.repository.ChatMessageRepository;
import vn.ute.qaute.repository.ConsultationRequestRepository;
import vn.ute.qaute.repository.UserRepository;
import vn.ute.qaute.service.ChatService;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConsultationRequestRepository requestRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           ConsultationRequestRepository requestRepository,
                           UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ChatMessage saveMessage(ChatMessageDto dto, Long senderId) {
        ConsultationRequest consultation = requestRepository.findById(dto.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên tư vấn với ID: " + dto.getConsultationId()));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + senderId));

        MessageType type = dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT;

        ChatMessage message = new ChatMessage(
                consultation,
                sender,
                dto.getContent(),
                dto.getMediaUrl(),
                type
        );

        return chatMessageRepository.save(message);
    }

    @Override
    public List<ChatMessage> getChatHistory(Long consultationId) {
        return chatMessageRepository.findByConsultationIdOrderBySentAtAsc(consultationId);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long consultationId, Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByConsultationIdOrderBySentAtAsc(consultationId);
        for (ChatMessage msg : messages) {
            if (!msg.getSender().getId().equals(userId) && Boolean.FALSE.equals(msg.getIsRead())) {
                msg.setIsRead(true);
                chatMessageRepository.save(msg);
            }
        }
    }
}
