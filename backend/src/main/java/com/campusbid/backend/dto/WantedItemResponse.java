package com.campusbid.backend.dto;

import com.campusbid.backend.model.WantedItem;
import com.campusbid.backend.model.WantedItemStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WantedItemResponse {

    private Long id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String itemName;
    private String category;
    private BigDecimal maxBudget;
    private String preferredCondition;
    private String additionalRequirements;
    private WantedItemStatus status;
    private long matchCount;
    private List<WantedMatchDto> matches = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WantedItemResponse() {
    }

    public static WantedItemResponse fromEntity(WantedItem item, List<WantedMatchDto> matches) {
        if (item == null) return null;
        WantedItemResponse res = new WantedItemResponse();
        res.setId(item.getId());
        res.setStudentId(item.getStudentId());
        res.setStudentName(item.getStudentName());
        res.setStudentEmail(item.getStudentEmail());
        res.setItemName(item.getItemName());
        res.setCategory(item.getCategory());
        res.setMaxBudget(item.getMaxBudget());
        res.setPreferredCondition(item.getPreferredCondition());
        res.setAdditionalRequirements(item.getAdditionalRequirements());
        res.setStatus(item.getStatus());
        res.setCreatedAt(item.getCreatedAt());
        res.setUpdatedAt(item.getUpdatedAt());
        if (matches != null) {
            res.setMatches(matches);
            res.setMatchCount(matches.size());
        }
        return res;
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

    public long getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(long matchCount) {
        this.matchCount = matchCount;
    }

    public List<WantedMatchDto> getMatches() {
        return matches;
    }

    public void setMatches(List<WantedMatchDto> matches) {
        this.matches = matches;
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
