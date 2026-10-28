package com.campusbid.backend.dto;

import java.math.BigDecimal;

public class CopilotBidProposal {

    private Long listingId;
    private String listingTitle;
    private BigDecimal currentPrice;
    private BigDecimal minBidIncrement;
    private BigDecimal minNextBid;
    private BigDecimal proposedBid;
    private BigDecimal maxBudget;
    private boolean requiresConfirmation = true; // Always requires explicit confirmation
    private boolean canAfford = true;
    private String warning;

    public CopilotBidProposal() {
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getListingTitle() {
        return listingTitle;
    }

    public void setListingTitle(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public BigDecimal getMinNextBid() {
        return minNextBid;
    }

    public void setMinNextBid(BigDecimal minNextBid) {
        this.minNextBid = minNextBid;
    }

    public BigDecimal getProposedBid() {
        return proposedBid;
    }

    public void setProposedBid(BigDecimal proposedBid) {
        this.proposedBid = proposedBid;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public boolean isCanAfford() {
        return canAfford;
    }

    public void setCanAfford(boolean canAfford) {
        this.canAfford = canAfford;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
