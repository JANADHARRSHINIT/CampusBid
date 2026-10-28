package com.campusbid.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Embeddable
public class BidRecord {

    @Column(name = "bidder_id", length = 100)
    private String bidderId;

    @Column(name = "bidder_name", length = 100)
    private String bidderName;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "bid_time")
    private LocalDateTime bidTime;

    public BidRecord() {
    }

    public BidRecord(String bidderId, String bidderName, BigDecimal amount, LocalDateTime bidTime) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = bidTime;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
