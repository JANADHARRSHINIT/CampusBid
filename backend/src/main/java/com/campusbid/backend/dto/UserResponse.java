package com.campusbid.backend.dto;

import com.campusbid.backend.model.Role;
import com.campusbid.backend.model.User;
import com.campusbid.backend.security.UserPrincipal;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    private Role role;
    private boolean emailVerified;
    private boolean emailNotificationsEnabled;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setStudentId(user.getStudentId());
        response.setRole(user.getRole());
        response.setEmailVerified(user.isEmailVerified());
        response.setEmailNotificationsEnabled(user.isEmailNotificationsEnabled());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public static UserResponse fromPrincipal(UserPrincipal principal) {
        if (principal == null) return null;
        UserResponse response = new UserResponse();
        response.setId(principal.getId());
        response.setFullName(principal.getFullName());
        response.setEmail(principal.getEmail());
        response.setStudentId(principal.getStudentId());
        response.setRole(principal.getRole());
        response.setEmailVerified(principal.isEmailVerified());
        response.setEmailNotificationsEnabled(true);
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
