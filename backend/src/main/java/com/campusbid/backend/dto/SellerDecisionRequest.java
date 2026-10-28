package com.campusbid.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SellerDecisionRequest {

    @NotBlank(message = "Decision is required (APPROVE or REJECT)")
    @Pattern(regexp = "(?i)^(APPROVE|REJECT)$", message = "Decision must be APPROVE or REJECT")
    private String decision;

    private String notes;

    public SellerDecisionRequest() {}

    public SellerDecisionRequest(String decision, String notes) {
        this.decision = decision;
        this.notes = notes;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
