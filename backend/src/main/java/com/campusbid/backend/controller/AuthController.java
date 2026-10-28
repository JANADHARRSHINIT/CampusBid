package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.LoginRequest;
import com.campusbid.backend.dto.RegisterRequest;
import com.campusbid.backend.dto.UserResponse;
import com.campusbid.backend.security.UserPrincipal;
import com.campusbid.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new student account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        UserResponse user = authService.register(request, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully. Welcome to CampusBid!", user));
    }

    /**
     * Log in with student/admin credentials and obtain server session.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        UserResponse user = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Signed in successfully", user));
    }

    /**
     * Get authenticated student profile across page refreshes.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.success("Unauthenticated", null));
        }

        UserResponse user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", user));
    }

    /**
     * Terminate authenticated session and log out.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Signed out successfully", null));
    }

    /**
     * Verify email via single-use token.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @RequestParam String token) {

        boolean verified = authService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Email verified successfully. You have full access to CampusBid.",
                    Map.of("verified", true)
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Verification link is invalid or has expired."
            ));
        }
    }
}
