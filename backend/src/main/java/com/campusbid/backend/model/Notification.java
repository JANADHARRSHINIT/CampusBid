package com.campusbid.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an in-app student notification in CampusBid.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_recipient_created", columnList = "recipient_id, created_at"),
    @Index(name = "idx_recipient_read", columnList = "recipient_id, is_read")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "related_listing_id")
    private Long relatedListingId;

    @Column(name = "related_listing_title", length = 255)
    private String relatedListingTitle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(String recipientId, String recipientName, String title, String message,
                        NotificationType type, Long relatedListingId, String relatedListingTitle) {
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedListingId = relatedListingId;
        this.relatedListingTitle = relatedListingTitle;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    // Getters and Setters

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
