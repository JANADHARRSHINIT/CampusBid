package com.campusbid.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CopilotConfirmBidRequest {

    @NotNull(message = "Listing ID is required.")
    private Long listingId;

    @NotNull(message = "Bid amount is required.")
    @DecimalMin(value = "0.01", message = "Bid amount must be positive.")
    private BigDecimal bidAmount;

    private BigDecimal maxBudget;

    public CopilotConfirmBidRequest() {
    }

    public CopilotConfirmBidRequest(Long listingId, BigDecimal bidAmount, BigDecimal maxBudget) {
        this.listingId = listingId;
        this.bidAmount = bidAmount;
        this.maxBudget = maxBudget;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }
}
