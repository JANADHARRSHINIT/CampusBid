package com.campusbid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CopilotChatRequest {

    @NotBlank(message = "Message cannot be empty.")
    @Size(max = 1000, message = "Message must not exceed 1000 characters.")
    private String message;

    private Long contextListingId;

    private BigDecimal maxBudget;

    public CopilotChatRequest() {
    }

    public CopilotChatRequest(String message, Long contextListingId, BigDecimal maxBudget) {
        this.message = message;
        this.contextListingId = contextListingId;
        this.maxBudget = maxBudget;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getContextListingId() {
        return contextListingId;
    }

    public void setContextListingId(Long contextListingId) {
        this.contextListingId = contextListingId;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }
}
