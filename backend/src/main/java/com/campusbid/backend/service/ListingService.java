package com.campusbid.backend.service;

import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.exception.ResourceNotFoundException;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.repository.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public ListingResponse createListing(CreateListingRequest request) {
        // Business logic validation
        if (request.getClosingTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Closing date and time must be in the future.");
        }
        if (request.getStartingPrice() == null || request.getStartingPrice().signum() <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than 0.");
        }
        if (request.getMinBidIncrement() == null || request.getMinBidIncrement().signum() <= 0) {
            throw new IllegalArgumentException("Minimum bid increment must be greater than 0.");
        }

        Listing listing = new Listing();
        listing.setTitle(request.getTitle().trim());
        listing.setCategory(request.getCategory().trim());
        listing.setDescription(request.getDescription().trim());
        listing.setCondition(request.getCondition());
        listing.setStartingPrice(request.getStartingPrice());
        listing.setMinBidIncrement(request.getMinBidIncrement());
        listing.setCurrentPrice(request.getStartingPrice());
        listing.setClosingTime(request.getClosingTime());
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setSellerId(request.getSellerId().trim());
        listing.setSellerName(request.getSellerName() != null ? request.getSellerName().trim() : request.getSellerId().trim());
        listing.setSellerEmail(request.getSellerEmail() != null ? request.getSellerEmail().trim() : null);

        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            listing.setPhotoUrls(request.getPhotoUrls());
        }

        Listing saved = listingRepository.save(listing);
        return ListingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getActiveListings(String category, String keyword) {
        List<Listing> listings;
        if ((category != null && !category.isBlank()) || (keyword != null && !keyword.isBlank())) {
            String cat = (category != null && !category.isBlank()) ? category.trim() : null;
            String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
            listings = listingRepository.searchListings(ListingStatus.ACTIVE, cat, kw);
        } else {
            listings = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.ACTIVE);
        }

        return listings.stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getAllListings(ListingStatus status, String category, String keyword) {
        List<Listing> listings;
        if (status != null || (category != null && !category.isBlank()) || (keyword != null && !keyword.isBlank())) {
            String cat = (category != null && !category.isBlank()) ? category.trim() : null;
            String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
            listings = listingRepository.searchListings(status, cat, kw);
        } else {
            listings = listingRepository.findAllByOrderByCreatedAtDesc();
        }

        return listings.stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));
        return ListingResponse.fromEntity(listing);
    }

    public ListingResponse updateListing(Long id, UpdateListingRequest request, String requesterStudentId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        // Ownership Verification
        validateSellerOwnership(listing, requesterStudentId);

        // Cannot edit if already cancelled or ended
        if (listing.getStatus() == ListingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled listings cannot be edited.");
        }

        if (request.getClosingTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Closing date and time must be in the future.");
        }
        if (request.getMinBidIncrement() == null || request.getMinBidIncrement().signum() <= 0) {
            throw new IllegalArgumentException("Minimum bid increment must be greater than 0.");
        }

        listing.setTitle(request.getTitle().trim());
        listing.setCategory(request.getCategory().trim());
        listing.setDescription(request.getDescription().trim());
        listing.setCondition(request.getCondition());
        listing.setMinBidIncrement(request.getMinBidIncrement());
        listing.setClosingTime(request.getClosingTime());

        if (request.getPhotoUrls() != null) {
            listing.getPhotoUrls().clear();
            listing.getPhotoUrls().addAll(request.getPhotoUrls());
        }

        Listing updated = listingRepository.save(listing);
        return ListingResponse.fromEntity(updated);
    }

    public ListingResponse cancelListing(Long id, String requesterStudentId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        // Ownership Verification
        validateSellerOwnership(listing, requesterStudentId);

        if (listing.getStatus() == ListingStatus.CANCELLED) {
            throw new IllegalArgumentException("Listing is already cancelled.");
        }

        // Cancel listing without deleting from database (soft cancellation)
        listing.setStatus(ListingStatus.CANCELLED);
        Listing updated = listingRepository.save(listing);
        return ListingResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsBySeller(String sellerId) {
        return listingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateSellerOwnership(Listing listing, String requesterStudentId) {
        if (requesterStudentId == null || requesterStudentId.trim().isEmpty()) {
            throw new UnauthorizedException("Access Denied: Missing student identifier header (X-Student-Id). Please specify who you are.");
        }
        if (!listing.getSellerId().trim().equalsIgnoreCase(requesterStudentId.trim())) {
            throw new UnauthorizedException("Access Denied: You cannot modify or cancel another student's listing. This item was listed by: " + listing.getSellerId());
        }
    }
}
