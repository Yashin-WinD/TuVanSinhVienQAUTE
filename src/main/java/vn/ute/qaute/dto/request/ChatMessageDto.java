package vn.ute.qaute.dto.request;

import vn.ute.qaute.entity.enums.MessageType;
import java.time.LocalDateTime;

public class ChatMessageDto {

    private Long consultationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderRole;
    private String content;
    private String mediaUrl;
    private MessageType messageType = MessageType.TEXT;
    private LocalDateTime sentAt;

    public ChatMessageDto() {
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

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
