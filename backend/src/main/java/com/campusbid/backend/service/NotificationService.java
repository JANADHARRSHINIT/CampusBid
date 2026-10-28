package com.campusbid.backend.service;

import com.campusbid.backend.dto.NotificationResponse;
import com.campusbid.backend.exception.ResourceNotFoundException;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.Notification;
import com.campusbid.backend.model.NotificationType;
import com.campusbid.backend.repository.NotificationRepository;
import com.campusbid.backend.model.User;
import com.campusbid.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
                               @Autowired(required = false) UserRepository userRepository,
                               @Autowired(required = false) EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Send/save a new notification directly to a recipient.
     */
    @Transactional
    public NotificationResponse sendNotification(
            String recipientId,
            String recipientName,
            String title,
            String message,
            NotificationType type,
            Long relatedListingId,
            String relatedListingTitle) {

        if (recipientId == null || recipientId.isBlank()) {
            log.warn("Attempted to send notification without recipientId: title={}", title);
            return null;
        }

        Notification notification = new Notification(
                recipientId.trim(),
                recipientName != null ? recipientName.trim() : null,
                title.trim(),
                message.trim(),
                type,
                relatedListingId,
                relatedListingTitle
        );

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent [id={}, recipientId={}, type={}, listingId={}]",
                saved.getId(), recipientId, type, relatedListingId);

        // Supplement with email notification asynchronously without risking transaction rollback
        dispatchEmailNotification(saved);

        return NotificationResponse.fromEntity(saved);
    }

    /**
     * Safe asynchronous email notification dispatch.
     */
    private void dispatchEmailNotification(Notification notification) {
        if (emailService == null || userRepository == null) {
            return;
        }
        try {
            User user = userRepository.findByStudentId(notification.getRecipientId())
                    .or(() -> userRepository.findByEmail(notification.getRecipientId()))
                    .orElse(null);

            if (user == null || !user.isEnabled() || !user.isEmailNotificationsEnabled()) {
                log.debug("Skipping email for recipient {}: user not found or emails disabled.", notification.getRecipientId());
                return;
            }

            String email = user.getEmail();
            String name = user.getFullName() != null ? user.getFullName() : notification.getRecipientName();
            String lotTitle = notification.getRelatedListingTitle() != null ? notification.getRelatedListingTitle() : "Campus Lot";
            Long lotId = notification.getRelatedListingId();

            switch (notification.getType()) {
                case OUTBID -> emailService.sendOutbidEmail(email, name, lotTitle, lotId, null);
                case BID_ACCEPTED -> emailService.sendBidAcceptedEmail(email, name, lotTitle, lotId, null);
                case BID_RECEIVED -> emailService.sendBidReceivedEmail(email, name, lotTitle, lotId, null, null);
                case AUCTION_WON -> emailService.sendAuctionWonEmail(email, name, lotTitle, lotId, null);
                case AUCTION_CLOSED, AWAITING_APPROVAL -> emailService.sendAuctionClosedEmail(email, name, lotTitle, lotId, true, null);
                case BID_APPROVED -> emailService.sendBidApprovedEmail(email, name, lotTitle, lotId, null);
                case BID_REJECTED -> emailService.sendBidRejectedEmail(email, name, lotTitle, lotId);
                case HANDOVER_CONFIRMED, SALE_COMPLETED -> emailService.sendSaleCompletedEmail(email, name, lotTitle, lotId, false);
                case WANTED_ITEM_MATCH -> emailService.sendWantedItemMatchEmail(email, name, lotTitle, lotId, null);
                default -> log.debug("No email dispatch configured for notification type: {}", notification.getType());
            }
        } catch (Exception ex) {
            log.warn("Could not dispatch email notification for [recipient={}, type={}]: {}",
                    notification.getRecipientId(), notification.getType(), ex.getMessage());
        }
    }

    /**
     * Send notification only if one with the same recipient, listingId, and type hasn't already been sent.
     * Prevents duplicate notifications during repeated closing checks or retries.
     */
    @Transactional
    public NotificationResponse sendNotificationIfNotExists(
            String recipientId,
            String recipientName,
            String title,
            String message,
            NotificationType type,
            Long relatedListingId,
            String relatedListingTitle) {

        if (recipientId == null || recipientId.isBlank()) {
            return null;
        }

        if (relatedListingId != null &&
                notificationRepository.existsByRecipientIdAndRelatedListingIdAndType(recipientId.trim(), relatedListingId, type)) {
            log.debug("Duplicate notification skipped [recipientId={}, listingId={}, type={}]",
                    recipientId, relatedListingId, type);
            return null;
        }

        return sendNotification(recipientId, recipientName, title, message, type, relatedListingId, relatedListingTitle);
    }

    /**
     * Fetch all notifications for a specific recipient, newest first.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForRecipient(String recipientId) {
        if (recipientId == null || recipientId.isBlank()) {
            throw new UnauthorizedException("Student ID is required to access notifications.");
        }

        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId.trim())
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get count of unread notifications for a recipient.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientId) {
        if (recipientId == null || recipientId.isBlank()) {
            return 0;
        }
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId.trim());
    }

    /**
     * Mark a single notification as read, validating that the requesting student owns it.
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new UnauthorizedException("Student ID is required to update notification read status.");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientId().equalsIgnoreCase(studentId.trim())) {
            throw new UnauthorizedException("You are not authorized to access or modify this notification.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    /**
     * Mark all unread notifications for the specified student as read.
     */
    @Transactional
    public int markAllAsRead(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new UnauthorizedException("Student ID is required to mark notifications as read.");
        }

        return notificationRepository.markAllAsRead(studentId.trim(), LocalDateTime.now());
    }
}
