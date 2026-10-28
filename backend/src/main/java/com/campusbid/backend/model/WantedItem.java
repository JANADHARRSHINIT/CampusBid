package com.campusbid.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wanted_items", indexes = {
    @Index(name = "idx_wanted_student", columnList = "student_id"),
    @Index(name = "idx_wanted_status", columnList = "status"),
    @Index(name = "idx_wanted_created", columnList = "created_at")
})
public class WantedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 80)
    private String studentId;

    @Column(name = "student_name", length = 120)
    private String studentName;

    @Column(name = "student_email", length = 120)
    private String studentEmail;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(length = 80)
    private String category;

    @Column(name = "max_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxBudget;

    @Column(name = "preferred_condition", length = 30)
    private String preferredCondition = "ANY";

    @Column(name = "additional_requirements", columnDefinition = "TEXT")
    private String additionalRequirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WantedItemStatus status = WantedItemStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WantedItem() {
    }

    public WantedItem(String studentId, String studentName, String studentEmail,
                      String itemName, String category, BigDecimal maxBudget,
                      String preferredCondition, String additionalRequirements) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.itemName = itemName;
        this.category = category;
        this.maxBudget = maxBudget;
        this.preferredCondition = (preferredCondition != null && !preferredCondition.isBlank()) ? preferredCondition : "ANY";
        this.additionalRequirements = additionalRequirements;
        this.status = WantedItemStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.preferredCondition == null || this.preferredCondition.isBlank()) {
            this.preferredCondition = "ANY";
        }
        if (this.status == null) {
            this.status = WantedItemStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getPreferredCondition() {
        return preferredCondition;
    }

    public void setPreferredCondition(String preferredCondition) {
        this.preferredCondition = preferredCondition;
    }

    public String getAdditionalRequirements() {
        return additionalRequirements;
    }

    public void setAdditionalRequirements(String additionalRequirements) {
        this.additionalRequirements = additionalRequirements;
    }

    public WantedItemStatus getStatus() {
        return status;
    }

    public void setStatus(WantedItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
