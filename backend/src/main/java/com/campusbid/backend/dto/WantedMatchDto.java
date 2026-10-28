package com.campusbid.backend.dto;

import com.campusbid.backend.model.WantedItemMatch;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WantedMatchDto {

    private Long id;
    private Long wantedItemId;
    private Long listingId;
    private String listingTitle;
    private BigDecimal listingPrice;
    private String listingPhoto;
    private LocalDateTime matchedAt;

    public WantedMatchDto() {
    }

    public static WantedMatchDto fromEntity(WantedItemMatch entity) {
        if (entity == null) return null;
        WantedMatchDto dto = new WantedMatchDto();
        dto.setId(entity.getId());
        dto.setWantedItemId(entity.getWantedItemId());
        dto.setListingId(entity.getListingId());
        dto.setListingTitle(entity.getListingTitle());
        dto.setListingPrice(entity.getListingPrice());
        dto.setListingPhoto(entity.getListingPhoto());
        dto.setMatchedAt(entity.getMatchedAt());
        return dto;
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
