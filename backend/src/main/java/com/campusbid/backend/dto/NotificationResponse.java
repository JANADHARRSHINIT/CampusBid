package com.campusbid.backend.dto;

import com.campusbid.backend.model.Notification;
import com.campusbid.backend.model.NotificationType;
import java.time.LocalDateTime;

/**
 * Response DTO for notifications delivered to frontend clients.
 */
public class NotificationResponse {

    private Long id;
    private String recipientId;
    private String recipientName;
    private String title;
    private String message;
    private NotificationType type;
    private Long relatedListingId;
    private String relatedListingTitle;
    private LocalDateTime createdAt;
    private boolean read;
    private LocalDateTime readAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) return null;
        NotificationResponse dto = new NotificationResponse();
        dto.setId(entity.getId());
        dto.setRecipientId(entity.getRecipientId());
        dto.setRecipientName(entity.getRecipientName());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setType(entity.getType());
        dto.setRelatedListingId(entity.getRelatedListingId());
        dto.setRelatedListingTitle(entity.getRelatedListingTitle());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRead(entity.isRead());
        dto.setReadAt(entity.getReadAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getRelatedListingId() {
        return relatedListingId;
    }

    public void setRelatedListingId(Long relatedListingId) {
        this.relatedListingId = relatedListingId;
    }

    public String getRelatedListingTitle() {
        return relatedListingTitle;
    }

    public void setRelatedListingTitle(String relatedListingTitle) {
        this.relatedListingTitle = relatedListingTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
