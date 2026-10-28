package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.NotificationResponse;
import com.campusbid.backend.security.UserPrincipal;
import com.campusbid.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String resolveStudentId(UserPrincipal principal, String headerStudentId) {
        if (principal != null) {
            return principal.getStudentId() != null ? principal.getStudentId() : principal.getEmail();
        }
        return (headerStudentId != null && !headerStudentId.isBlank()) ? headerStudentId.trim() : null;
    }

    /**
     * Retrieve all notifications for the active student, newest first.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to access notifications."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId);
        List<NotificationResponse> notifications = notificationService.getNotificationsForRecipient(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications retrieved successfully",
                notifications
        ));
    }

    /**
     * Retrieve the count of unread notifications for the active student.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to access notification count."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId);
        long count = notificationService.getUnreadCount(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Unread notification count retrieved",
                count
        ));
    }

    /**
     * Mark a single notification as read.
     */
    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to update notifications."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId);
        NotificationResponse updated = notificationService.markAsRead(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Notification marked as read",
                updated
        ));
    }

    /**
     * Mark all unread notifications for the student as read.
     */
    @RequestMapping(value = "/mark-all-read", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to update notifications."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId);
        int updatedCount = notificationService.markAllAsRead(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success(
                "All notifications marked as read",
                updatedCount
        ));
    }
}
