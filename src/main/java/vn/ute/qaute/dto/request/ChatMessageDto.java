package vn.ute.qaute.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import vn.ute.qaute.entity.enums.MessageType;
import java.time.LocalDateTime;

public class ChatMessageDto {

    private Long id;
    private Long consultationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderRole;
    private String content;
    private String mediaUrl;
    private MessageType messageType = MessageType.TEXT;
    private Boolean isRead = false;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    public ChatMessageDto() {
    }

    public static ChatMessageDto fromEntity(vn.ute.qaute.entity.ChatMessage entity) {
        if (entity == null) return null;
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(entity.getId());
        dto.setConsultationId(entity.getConsultation() != null ? entity.getConsultation().getId() : null);
        if (entity.getSender() != null) {
            dto.setSenderId(entity.getSender().getId());
            dto.setSenderName(entity.getSender().getFullName());
            dto.setSenderAvatar(entity.getSender().getAvatarUrl());
            dto.setSenderRole(entity.getSender().getRole() != null ? entity.getSender().getRole().name() : null);
        }
        dto.setContent(entity.getContent());
        dto.setMediaUrl(entity.getMediaUrl());
        dto.setMessageType(entity.getMessageType());
        dto.setIsRead(entity.getIsRead());
        dto.setSentAt(entity.getSentAt());
        return dto;
    }

    public ChatMessageDto(Long consultationId, Long senderId, String senderName, String senderAvatar,
                          String senderRole, String content, String mediaUrl, MessageType messageType) {
        this.consultationId = consultationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.senderRole = senderRole;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.sentAt = LocalDateTime.now();
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
