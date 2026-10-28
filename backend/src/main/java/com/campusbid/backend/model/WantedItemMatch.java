package com.campusbid.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wanted_item_matches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_wanted_listing_match", columnNames = {"wanted_item_id", "listing_id"})
}, indexes = {
    @Index(name = "idx_match_wanted_id", columnList = "wanted_item_id"),
    @Index(name = "idx_match_listing_id", columnList = "listing_id")
})
public class WantedItemMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wanted_item_id", nullable = false)
    private Long wantedItemId;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "listing_title", length = 150)
    private String listingTitle;

    @Column(name = "listing_price", precision = 12, scale = 2)
    private BigDecimal listingPrice;

    @Column(name = "listing_photo", length = 500)
    private String listingPhoto;

    @Column(name = "matched_at", nullable = false, updatable = false)
    private LocalDateTime matchedAt;

    public WantedItemMatch() {
    }

    public WantedItemMatch(Long wantedItemId, Long listingId, String listingTitle,
                           BigDecimal listingPrice, String listingPhoto) {
        this.wantedItemId = wantedItemId;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
        this.listingPrice = listingPrice;
        this.listingPhoto = listingPhoto;
        this.matchedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.matchedAt == null) {
            this.matchedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWantedItemId() {
        return wantedItemId;
    }

    public void setWantedItemId(Long wantedItemId) {
        this.wantedItemId = wantedItemId;
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

    public BigDecimal getListingPrice() {
        return listingPrice;
    }

    public void setListingPrice(BigDecimal listingPrice) {
        this.listingPrice = listingPrice;
    }

    public String getListingPhoto() {
        return listingPhoto;
    }

    public void setListingPhoto(String listingPhoto) {
        this.listingPhoto = listingPhoto;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }
}
