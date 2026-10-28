package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.service.FileStorageService;
import com.campusbid.backend.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class ListingController {

    private final ListingService listingService;
    private final FileStorageService fileStorageService;

    public ListingController(ListingService listingService, FileStorageService fileStorageService) {
        this.listingService = listingService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload one or more item photos (validates size and types).
     */
    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        List<String> storedUrls = fileStorageService.storeFiles(files);
        return ResponseEntity.ok(ApiResponse.success("Photos uploaded successfully", storedUrls));
    }

    /**
     * Create a new item listing.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @RequestHeader(value = "X-Student-Id", required = false) String headerStudentId
    ) {
        // Fallback to request header if sellerId wasn't in body or vice versa
        if ((request.getSellerId() == null || request.getSellerId().isBlank()) && headerStudentId != null) {
            request.setSellerId(headerStudentId);
        }
        ListingResponse created = listingService.createListing(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing created successfully", created));
    }

    /**
     * View active listings with multi-criteria filtering and keyword search.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getActiveListings(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) com.campusbid.backend.model.ItemCondition condition,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Boolean closingSoon,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy
    ) {
        List<ListingResponse> listings = listingService.getActiveListings(
                category, condition, minPrice, maxPrice, closingSoon, keyword, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success("Active listings retrieved", listings));
    }

    /**
     * View all listings including CANCELLED and ENDED with multi-criteria filtering.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getAllListings(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) com.campusbid.backend.model.ItemCondition condition,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Boolean closingSoon,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy
    ) {
        List<ListingResponse> listings = listingService.getAllListings(
                status, category, condition, minPrice, maxPrice, closingSoon, keyword, sortBy
        );
        return ResponseEntity.ok(ApiResponse.success("All listings retrieved", listings));
    }

    /**
     * View a single listing by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListingById(@PathVariable Long id) {
        ListingResponse listing = listingService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success("Listing retrieved", listing));
    }

    /**
     * Edit an existing listing (restricted to seller).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank()) ? studentId : paramSellerId;
        ListingResponse updated = listingService.updateListing(id, request, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully", updated));
    }

    /**
     * Cancel an existing listing after confirmation (restricted to seller).
     * Soft cancellation keeps the record in the database.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelListingPost(
            @PathVariable Long id,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank()) ? studentId : paramSellerId;
        ListingResponse cancelled = listingService.cancelListing(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Listing cancelled successfully", cancelled));
    }

    /**
     * Support HTTP DELETE for cancelling listings as well.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelListingDelete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank()) ? studentId : paramSellerId;
        ListingResponse cancelled = listingService.cancelListing(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Listing cancelled successfully", cancelled));
    }

    /**
     * View listings created by a specific student.
     */
    @GetMapping("/my-listings")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getMyListings(
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank()) ? studentId : paramSellerId;
        if (effectiveStudentId == null || effectiveStudentId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Student ID is required via X-Student-Id header or sellerId parameter."));
        }
        List<ListingResponse> listings = listingService.getListingsBySeller(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Seller listings retrieved", listings));
    }
}
