package com.campusbid.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ItemCondition condition;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_photos", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "photo_url", length = 500)
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "starting_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "min_bid_increment", nullable = false, precision = 10, scale = 2)
    private BigDecimal minBidIncrement;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_bids", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderBy("amount DESC")
    private List<BidRecord> bidHistory = new ArrayList<>();

    @Column(name = "closing_time", nullable = false)
    private LocalDateTime closingTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "seller_id", nullable = false, length = 100)
    private String sellerId;

    @Column(name = "seller_name", length = 100)
    private String sellerName;

    @Column(name = "seller_email", length = 100)
    private String sellerEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "winning_bidder_id", length = 100)
    private String winningBidderId;

    @Column(name = "winning_bidder_name", length = 100)
    private String winningBidderName;

    @Column(name = "winning_bid_amount", precision = 10, scale = 2)
    private BigDecimal winningBidAmount;

    @Column(name = "winning_bid_time")
    private LocalDateTime winningBidTime;

    @Column(name = "seller_decision", length = 20)
    private String sellerDecision;

    @Column(name = "seller_decision_time")
    private LocalDateTime sellerDecisionTime;

    @Column(name = "seller_decision_notes", length = 500)
    private String sellerDecisionNotes;

    @Column(name = "handover_confirmed_by_seller")
    private Boolean handoverConfirmedBySeller = false;

    @Column(name = "handover_confirmed_by_winner")
    private Boolean handoverConfirmedByWinner = false;

    @Column(name = "handover_time")
    private LocalDateTime handoverTime;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.currentPrice == null) {
            this.currentPrice = this.startingPrice;
        }
        if (this.status == null) {
            this.status = ListingStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Default constructor
    public Listing() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public void setCondition(ItemCondition condition) {
        this.condition = condition;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public List<BidRecord> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidRecord> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public LocalDateTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalDateTime closingTime) {
        this.closingTime = closingTime;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
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

    public String getWinningBidderId() {
        return winningBidderId;
    }

    public void setWinningBidderId(String winningBidderId) {
        this.winningBidderId = winningBidderId;
    }

    public String getWinningBidderName() {
        return winningBidderName;
    }

    public void setWinningBidderName(String winningBidderName) {
        this.winningBidderName = winningBidderName;
    }

    public BigDecimal getWinningBidAmount() {
        return winningBidAmount;
    }

    public void setWinningBidAmount(BigDecimal winningBidAmount) {
        this.winningBidAmount = winningBidAmount;
    }

    public LocalDateTime getWinningBidTime() {
        return winningBidTime;
    }

    public void setWinningBidTime(LocalDateTime winningBidTime) {
        this.winningBidTime = winningBidTime;
    }

    public String getSellerDecision() {
        return sellerDecision;
    }

    public void setSellerDecision(String sellerDecision) {
        this.sellerDecision = sellerDecision;
    }

    public LocalDateTime getSellerDecisionTime() {
        return sellerDecisionTime;
    }

    public void setSellerDecisionTime(LocalDateTime sellerDecisionTime) {
        this.sellerDecisionTime = sellerDecisionTime;
    }

    public String getSellerDecisionNotes() {
        return sellerDecisionNotes;
    }

    public void setSellerDecisionNotes(String sellerDecisionNotes) {
        this.sellerDecisionNotes = sellerDecisionNotes;
    }

    public Boolean getHandoverConfirmedBySeller() {
        return handoverConfirmedBySeller != null && handoverConfirmedBySeller;
    }

    public void setHandoverConfirmedBySeller(Boolean handoverConfirmedBySeller) {
        this.handoverConfirmedBySeller = handoverConfirmedBySeller;
    }

    public Boolean getHandoverConfirmedByWinner() {
        return handoverConfirmedByWinner != null && handoverConfirmedByWinner;
    }

    public void setHandoverConfirmedByWinner(Boolean handoverConfirmedByWinner) {
        this.handoverConfirmedByWinner = handoverConfirmedByWinner;
    }

    public LocalDateTime getHandoverTime() {
        return handoverTime;
    }

    public void setHandoverTime(LocalDateTime handoverTime) {
        this.handoverTime = handoverTime;
    }
}
