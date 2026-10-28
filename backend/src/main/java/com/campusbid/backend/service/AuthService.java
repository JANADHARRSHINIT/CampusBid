package com.campusbid.backend.service;

import com.campusbid.backend.dto.LoginRequest;
import com.campusbid.backend.dto.RegisterRequest;
import com.campusbid.backend.dto.UserResponse;
import com.campusbid.backend.model.Role;
import com.campusbid.backend.model.User;
import com.campusbid.backend.repository.UserRepository;
import com.campusbid.backend.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Register a new student account.
     * Rejects duplicate emails, enforces BCrypt hashing, and restricts public registration to STUDENT role.
     */
    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        String studentId = request.getStudentId() != null && !request.getStudentId().isBlank()
                ? request.getStudentId().trim()
                : "student-" + (System.currentTimeMillis() % 100000);

        if (userRepository.existsByStudentId(studentId)) {
            studentId = studentId + "-" + (int)(Math.random() * 900 + 100);
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT); // Strictly enforce STUDENT role; ignore any external attempts
        user.setStudentId(studentId);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setEmailNotificationsEnabled(true);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        User saved = userRepository.save(user);
        log.info("Registered new student user [id={}, email='{}', studentId='{}']",
                saved.getId(), saved.getEmail(), saved.getStudentId());

        // Automatically establish authenticated session upon successful registration
        authenticateAndSetSession(saved.getEmail(), request.getPassword(), httpRequest, httpResponse);

        return UserResponse.fromEntity(saved);
    }

    /**
     * Authenticate student/admin credentials and initialize secure server session with HttpOnly cookie.
     */
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticateAndSetSession(normalizedEmail, request.getPassword(), httpRequest, httpResponse);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            log.info("Successful login for student/user [id={}, email='{}', role={}]",
                    principal.getId(), principal.getEmail(), principal.getRole());
            return UserResponse.fromPrincipal(principal);
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email='{}': Invalid credentials", normalizedEmail);
            throw new BadCredentialsException("Invalid email address or password.");
        }
    }

    /**
     * Helper to authenticate with AuthenticationManager and persist SecurityContext to session.
     */
    private Authentication authenticateAndSetSession(String email, String rawPassword,
                                                     HttpServletRequest request, HttpServletResponse response) {
        Authentication authReq = new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication authRes = authenticationManager.authenticate(authReq);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authRes);
        SecurityContextHolder.setContext(context);

        if (request != null && response != null) {
            securityContextRepository.saveContext(context, request, response);
        }

        return authRes;
    }

    /**
     * Return safe user profile of currently authenticated principal.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            return null;
        }

        return userRepository.findById(principal.getId())
                .map(UserResponse::fromEntity)
                .orElse(UserResponse.fromPrincipal(principal));
    }

    /**
     * Log out active user and invalidate HTTP session.
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        if (response != null) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", null);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    /**
     * Verify email with single-use verification token.
     */
    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return userRepository.findByVerificationToken(token.trim())
                .filter(u -> u.getVerificationTokenExpiry() == null || u.getVerificationTokenExpiry().isAfter(LocalDateTime.now()))
                .map(u -> {
                    u.setEmailVerified(true);
                    u.setVerificationToken(null);
                    u.setVerificationTokenExpiry(null);
                    userRepository.save(u);
                    log.info("Email verified successfully for user [id={}, email='{}']", u.getId(), u.getEmail());
                    return true;
                })
                .orElse(false);
    }
}
