package com.campusbid.backend.dto;

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
    private LocalDateTime closingTime;
    private ListingStatus status;
    private String sellerId;
    private String sellerName;
    private String sellerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isExpired;
    private long secondsRemaining;

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
        dto.setClosingTime(listing.getClosingTime());
        dto.setStatus(listing.getStatus());
        dto.setSellerId(listing.getSellerId());
        dto.setSellerName(listing.getSellerName());
        dto.setSellerEmail(listing.getSellerEmail());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setUpdatedAt(listing.getUpdatedAt());

        LocalDateTime now = LocalDateTime.now();
        if (listing.getClosingTime() != null) {
            boolean expired = now.isAfter(listing.getClosingTime());
            dto.setExpired(expired);
            if (expired) {
                dto.setSecondsRemaining(0);
                if (listing.getStatus() == ListingStatus.ACTIVE) {
                    dto.setStatus(ListingStatus.ENDED);
                }
            } else {
                dto.setSecondsRemaining(Duration.between(now, listing.getClosingTime()).getSeconds());
            }
        }
        return dto;
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

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }
}
