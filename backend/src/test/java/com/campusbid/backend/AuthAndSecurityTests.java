package com.campusbid.backend;

import com.campusbid.backend.dto.LoginRequest;
import com.campusbid.backend.dto.RegisterRequest;
import com.campusbid.backend.dto.UserResponse;
import com.campusbid.backend.model.Role;
import com.campusbid.backend.model.User;
import com.campusbid.backend.repository.UserRepository;
import com.campusbid.backend.service.AuthService;
import com.campusbid.backend.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthAndSecurityTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Test
    @DisplayName("1. Register student and verify user record saved in MySQL with BCrypt hash")
    void testRegisterStudentSuccess() {
        String testEmail = "newstudent_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest req = new RegisterRequest("Sarah Connor", testEmail, "Terminator123!", "student-sarah");

        UserResponse res = authService.register(req, null, null);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("Sarah Connor", res.getFullName());
        assertEquals(testEmail, res.getEmail());
        assertEquals(Role.STUDENT, res.getRole());
        assertEquals("student-sarah", res.getStudentId());

        // Verify password in DB is BCrypt hashed
        Optional<User> savedUser = userRepository.findById(res.getId());
        assertTrue(savedUser.isPresent());
        User u = savedUser.get();
        assertNotEquals("Terminator123!", u.getPassword());
        assertTrue(u.getPassword().startsWith("$2a$") || u.getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("Terminator123!", u.getPassword()));
    }

    @Test
    @DisplayName("2. Confirm duplicate registration with same email is rejected")
    void testDuplicateRegistrationRejected() {
        String testEmail = "duplicate_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest req1 = new RegisterRequest("Student One", testEmail, "Password123!", "student-dup-1");
        authService.register(req1, null, null);

        RegisterRequest req2 = new RegisterRequest("Student Two", testEmail, "Password123!", "student-dup-2");
        assertThrows(IllegalArgumentException.class, () -> authService.register(req2, null, null));
    }

    @Test
    @DisplayName("3. Public registration strictly enforces STUDENT role, never ADMIN")
    void testPublicRegistrationRestrictedToStudent() {
        String testEmail = "hacker_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest req = new RegisterRequest("Mr Hacker", testEmail, "SecretPass123!", "student-hacker");

        UserResponse res = authService.register(req, null, null);
        assertEquals(Role.STUDENT, res.getRole());

        User dbUser = userRepository.findById(res.getId()).orElseThrow();
        assertEquals(Role.STUDENT, dbUser.getRole());
    }

    @Test
    @DisplayName("4. Login with valid credentials succeeds and returns safe user profile")
    void testLoginSuccess() {
        String testEmail = "loginuser_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest reg = new RegisterRequest("John Doe", testEmail, "ValidPass123!", "student-johndoe");
        authService.register(reg, null, null);

        LoginRequest loginReq = new LoginRequest(testEmail, "ValidPass123!");
        UserResponse loginRes = authService.login(loginReq, null, null);

        assertNotNull(loginRes);
        assertEquals("John Doe", loginRes.getFullName());
        assertEquals(testEmail, loginRes.getEmail());
    }

    @Test
    @DisplayName("5. Login with invalid password fails with BadCredentialsException")
    void testLoginInvalidPasswordFails() {
        String testEmail = "wrongpass_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest reg = new RegisterRequest("Jane Doe", testEmail, "CorrectPass123!", "student-janedoe");
        authService.register(reg, null, null);

        LoginRequest badLogin = new LoginRequest(testEmail, "WrongPassword!");
        assertThrows(BadCredentialsException.class, () -> authService.login(badLogin, null, null));
    }

    @Test
    @DisplayName("6. Single-use email verification token flow succeeds")
    void testEmailVerificationFlow() {
        String testEmail = "verify_" + System.currentTimeMillis() + "@campus.edu";
        RegisterRequest reg = new RegisterRequest("Test Verification", testEmail, "ValidPass123!", null);
        UserResponse userRes = authService.register(reg, null, null);

        User user = userRepository.findById(userRes.getId()).orElseThrow();
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getVerificationToken());

        // Verify with valid token
        boolean verified = authService.verifyEmail(user.getVerificationToken());
        assertTrue(verified);

        User updatedUser = userRepository.findById(userRes.getId()).orElseThrow();
        assertTrue(updatedUser.isEmailVerified());
        assertNull(updatedUser.getVerificationToken());

        // Reusing same token must fail
        boolean secondAttempt = authService.verifyEmail("invalid-token-123");
        assertFalse(secondAttempt);
    }

    @Test
    @DisplayName("7. EmailService generates outbid, accepted, won, and seller templates without exception")
    void testEmailTemplatesDispatch() {
        assertDoesNotThrow(() -> {
            emailService.sendOutbidEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L, new BigDecimal("600.00"));
            emailService.sendBidAcceptedEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L, new BigDecimal("600.00"));
            emailService.sendBidReceivedEmail("seller@campus.edu", "Maya", "MacBook Air", 1L, new BigDecimal("600.00"), "Alex");
            emailService.sendAuctionWonEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L, new BigDecimal("600.00"));
            emailService.sendAuctionClosedEmail("seller@campus.edu", "Maya", "MacBook Air", 1L, true, new BigDecimal("600.00"));
            emailService.sendBidApprovedEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L, new BigDecimal("600.00"));
            emailService.sendBidRejectedEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L);
            emailService.sendSaleCompletedEmail("buyer@campus.edu", "Alex", "MacBook Air", 1L, false);
            emailService.sendTestEmail("admin@campusbid.edu");
        });
    }
}
