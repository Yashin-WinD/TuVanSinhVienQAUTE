package vn.ute.qaute.service;

import vn.ute.qaute.dto.request.ChatMessageDto;
import vn.ute.qaute.entity.ChatMessage;

import java.util.List;

public interface ChatService {
    ChatMessage saveMessage(ChatMessageDto dto, Long senderId);
    List<ChatMessage> getChatHistory(Long consultationId);
    void markMessagesAsRead(Long consultationId, Long userId);
}
