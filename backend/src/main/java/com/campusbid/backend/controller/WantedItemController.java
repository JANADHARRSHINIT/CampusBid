package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.WantedItemRequest;
import com.campusbid.backend.dto.WantedItemResponse;
import com.campusbid.backend.model.WantedItemStatus;
import com.campusbid.backend.security.UserPrincipal;
import com.campusbid.backend.service.WantedItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wanted-items")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000"}, allowCredentials = "true")
public class WantedItemController {

    private final WantedItemService wantedItemService;

    public WantedItemController(WantedItemService wantedItemService) {
        this.wantedItemService = wantedItemService;
    }

    private String getEffectiveStudentId(UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        if (principal.getStudentId() != null && !principal.getStudentId().isBlank()) {
            return principal.getStudentId().trim();
        }
        return principal.getEmail() != null ? principal.getEmail().trim() : null;
    }

    @GetMapping
    public ResponseEntity<?> getMyWantedItems(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to view wanted items."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentEmail = principal.getEmail();
        List<WantedItemResponse> items = wantedItemService.getStudentWantedItems(studentId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success("Wanted items retrieved successfully", items));
    }

    @PostMapping
    public ResponseEntity<?> createWantedItem(
            @Valid @RequestBody WantedItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to create wanted item alerts."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentName = principal.getFullName();
        String studentEmail = principal.getEmail();

        WantedItemResponse created = wantedItemService.createWantedItem(request, studentId, studentName, studentEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wanted item alert created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWantedItemById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentEmail = principal.getEmail();
        WantedItemResponse item = wantedItemService.getWantedItemById(id, studentId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success("Wanted item details retrieved", item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWantedItem(
            @PathVariable Long id,
            @Valid @RequestBody WantedItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentEmail = principal.getEmail();
        WantedItemResponse updated = wantedItemService.updateWantedItem(id, request, studentId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success("Wanted item alert updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) WantedItemStatus status,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentEmail = principal.getEmail();

        WantedItemStatus targetStatus = status;
        if (targetStatus == null && body != null && body.containsKey("status") && body.get("status") != null) {
            try {
                targetStatus = WantedItemStatus.valueOf(body.get("status").toString().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        WantedItemResponse updated = wantedItemService.toggleStatus(id, targetStatus, studentId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success("Wanted item status updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWantedItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }
        String studentId = getEffectiveStudentId(principal);
        String studentEmail = principal.getEmail();
        wantedItemService.deleteWantedItem(id, studentId, studentEmail);
        return ResponseEntity.ok(ApiResponse.success("Wanted item request removed successfully", null));
    }
}
