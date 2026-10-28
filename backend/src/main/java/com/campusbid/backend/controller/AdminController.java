package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.UserResponse;
import com.campusbid.backend.repository.UserRepository;
import com.campusbid.backend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class AdminController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public AdminController(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * Send test email to verify SMTP configuration and delivery.
     * Restricted strictly to administrators.
     */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @RequestParam(required = false, defaultValue = "admin@campusbid.edu") String email) {

        boolean sent = emailService.sendTestEmail(email);
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Test email successfully dispatched to " + email,
                    Map.of("delivered", true, "recipient", email)
            ));
        } else {
            return ResponseEntity.ok(ApiResponse.success(
                    "Test email attempted. (Note: Email simulation mode active if SMTP is not configured or disabled).",
                    Map.of("delivered", false, "recipient", email, "note", "Check server logs or application.properties SMTP settings.")
            ));
        }
    }

    /**
     * List registered students and accounts (never exposes passwords or hashes).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
}
