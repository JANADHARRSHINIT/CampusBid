package com.campusbid.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class WantedItemRequest {

    @NotBlank(message = "Item name or search keywords are required.")
    @Size(min = 2, max = 150, message = "Item name must be between 2 and 150 characters.")
    private String itemName;

    @Size(max = 80, message = "Category cannot exceed 80 characters.")
    private String category;

    @NotNull(message = "Maximum budget is required.")
    @DecimalMin(value = "1.00", message = "Maximum budget must be at least ₹1.00.")
    private BigDecimal maxBudget;

    private String preferredCondition = "ANY";

    @Size(max = 2000, message = "Additional requirements cannot exceed 2000 characters.")
    private String additionalRequirements;

    public WantedItemRequest() {
    }

    public WantedItemRequest(String itemName, String category, BigDecimal maxBudget,
                             String preferredCondition, String additionalRequirements) {
        this.itemName = itemName;
        this.category = category;
        this.maxBudget = maxBudget;
        this.preferredCondition = preferredCondition;
        this.additionalRequirements = additionalRequirements;
    }

    // Getters and Setters

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
}
