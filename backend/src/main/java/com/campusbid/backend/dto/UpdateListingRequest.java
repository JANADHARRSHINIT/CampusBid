package com.campusbid.backend.dto;

import com.campusbid.backend.model.ItemCondition;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UpdateListingRequest {

    @NotBlank(message = "Item title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 3000, message = "Description must be between 10 and 3000 characters")
    private String description;

    @NotNull(message = "Item condition is required")
    private ItemCondition condition;

    @NotNull(message = "Minimum bid increment is required")
    @DecimalMin(value = "0.01", message = "Minimum bid increment must be greater than 0")
    private BigDecimal minBidIncrement;

    @NotNull(message = "Closing date and time is required")
    @Future(message = "Closing date and time must be in the future")
    private LocalDateTime closingTime;

    private List<String> photoUrls = new ArrayList<>();

    public UpdateListingRequest() {
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

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public LocalDateTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalDateTime closingTime) {
        this.closingTime = closingTime;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
}
