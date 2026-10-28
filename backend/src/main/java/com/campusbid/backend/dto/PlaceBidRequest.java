package com.campusbid.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PlaceBidRequest {

    @NotNull(message = "Bid amount is required.")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than 0.")
    private BigDecimal amount;

    private String bidderId;
    private String bidderName;

    public PlaceBidRequest() {
    }

    public PlaceBidRequest(BigDecimal amount, String bidderId, String bidderName) {
        this.amount = amount;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }
}
