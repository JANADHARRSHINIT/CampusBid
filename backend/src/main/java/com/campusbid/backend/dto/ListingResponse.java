package com.campusbid.backend.dto;

import com.campusbid.backend.model.BidRecord;
import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ListingResponse {

    private Long id;
    private String title;
    private String category;
    private String description;
    private ItemCondition condition;
    private List<String> photoUrls = new ArrayList<>();
    private BigDecimal startingPrice;
    private BigDecimal minBidIncrement;
    private BigDecimal currentPrice;
    private BigDecimal currentLeadingBid;
    private BigDecimal minNextBid;
    private List<BidRecord> bidHistory = new ArrayList<>();
    private int totalBids;
    private LocalDateTime closingTime;
    private ListingStatus status;
    private String sellerId;
    private String sellerName;
    private String sellerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isExpired;
    private boolean isClosed;
    private long secondsRemaining;

    private String winningBidderId;
    private String winningBidderName;
    private BigDecimal winningBidAmount;
    private LocalDateTime winningBidTime;
    private String sellerDecision;
    private LocalDateTime sellerDecisionTime;
    private String sellerDecisionNotes;
    private Boolean handoverConfirmedBySeller;
    private Boolean handoverConfirmedByWinner;
    private LocalDateTime handoverTime;

    public ListingResponse() {
    }

    public static ListingResponse fromEntity(Listing listing) {
        ListingResponse dto = new ListingResponse();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setCategory(listing.getCategory());
        dto.setDescription(listing.getDescription());
        dto.setCondition(listing.getCondition());
        dto.setPhotoUrls(listing.getPhotoUrls() != null ? new ArrayList<>(listing.getPhotoUrls()) : new ArrayList<>());
        dto.setStartingPrice(listing.getStartingPrice());
        dto.setMinBidIncrement(listing.getMinBidIncrement());
        dto.setCurrentPrice(listing.getCurrentPrice());
        boolean hasBids = listing.getBidHistory() != null && !listing.getBidHistory().isEmpty();
        dto.setCurrentLeadingBid(listing.getCurrentPrice());

        BigDecimal nextBid;
        if (!hasBids) {
            nextBid = listing.getStartingPrice();
        } else {
            BigDecimal inc = listing.getMinBidIncrement() != null ? listing.getMinBidIncrement() : BigDecimal.ONE;
            nextBid = listing.getCurrentPrice() != null ? listing.getCurrentPrice().add(inc) : listing.getStartingPrice();
        }
        dto.setMinNextBid(nextBid);

        if (hasBids) {
            dto.setBidHistory(new ArrayList<>(listing.getBidHistory()));
            dto.setTotalBids(listing.getBidHistory().size());
        } else {
            dto.setBidHistory(new ArrayList<>());
            dto.setTotalBids(0);
        }

        dto.setClosingTime(listing.getClosingTime());
        dto.setStatus(listing.getStatus());
        dto.setSellerId(listing.getSellerId());
        dto.setSellerName(listing.getSellerName());
        dto.setSellerEmail(listing.getSellerEmail());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setUpdatedAt(listing.getUpdatedAt());

        // Winner and sale management data
        dto.setWinningBidderId(listing.getWinningBidderId());
        dto.setWinningBidderName(listing.getWinningBidderName());
        dto.setWinningBidAmount(listing.getWinningBidAmount());
        dto.setWinningBidTime(listing.getWinningBidTime());
        dto.setSellerDecision(listing.getSellerDecision());
        dto.setSellerDecisionTime(listing.getSellerDecisionTime());
        dto.setSellerDecisionNotes(listing.getSellerDecisionNotes());
        dto.setHandoverConfirmedBySeller(listing.getHandoverConfirmedBySeller());
        dto.setHandoverConfirmedByWinner(listing.getHandoverConfirmedByWinner());
        dto.setHandoverTime(listing.getHandoverTime());

        LocalDateTime now = LocalDateTime.now();
        if (listing.getClosingTime() != null) {
            boolean expired = now.isAfter(listing.getClosingTime());
            dto.setExpired(expired);
            if (expired) {
                dto.setSecondsRemaining(0);
                if (listing.getStatus() == ListingStatus.ACTIVE) {
                    // Derive dynamic closing state if not yet persisted
                    if (hasBids) {
                        dto.setStatus(ListingStatus.AWAITING_SELLER_APPROVAL);
                        // Identify top bidder if not yet set
                        if (dto.getWinningBidderId() == null && !listing.getBidHistory().isEmpty()) {
                            BidRecord top = listing.getBidHistory().get(0);
                            dto.setWinningBidderId(top.getBidderId());
                            dto.setWinningBidderName(top.getBidderName());
                            dto.setWinningBidAmount(top.getAmount());
                            dto.setWinningBidTime(top.getBidTime());
                        }
                    } else {
                        dto.setStatus(ListingStatus.UNSOLD);
                    }
                }
            } else {
                dto.setSecondsRemaining(Duration.between(now, listing.getClosingTime()).getSeconds());
            }
        }
        dto.setClosed(dto.isExpired() || dto.getStatus() != ListingStatus.ACTIVE);
        return dto;
    }

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

    public BigDecimal getCurrentLeadingBid() {
        return currentLeadingBid;
    }

    public void setCurrentLeadingBid(BigDecimal currentLeadingBid) {
        this.currentLeadingBid = currentLeadingBid;
    }

    public BigDecimal getMinNextBid() {
        return minNextBid;
    }

    public void setMinNextBid(BigDecimal minNextBid) {
        this.minNextBid = minNextBid;
    }

    public List<BidRecord> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidRecord> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
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

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
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
