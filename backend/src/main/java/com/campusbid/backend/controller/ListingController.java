package com.campusbid.backend.controller;

import com.campusbid.backend.dto.ApiResponse;
import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.security.UserPrincipal;
import com.campusbid.backend.service.FileStorageService;
import com.campusbid.backend.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private String resolveStudentId(UserPrincipal principal, String headerStudentId, String paramStudentId) {
        if (principal != null) {
            return principal.getStudentId() != null ? principal.getStudentId() : principal.getEmail();
        }
        if (headerStudentId != null && !headerStudentId.isBlank()) {
            return headerStudentId.trim();
        }
        if (paramStudentId != null && !paramStudentId.isBlank()) {
            return paramStudentId.trim();
        }
        return null;
    }

    /**
     * Upload one or more item photos (validates size and types).
     */
    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to upload photos."));
        }
        List<String> storedUrls = fileStorageService.storeFiles(files);
        return ResponseEntity.ok(ApiResponse.success("Photos uploaded successfully", storedUrls));
    }

    /**
     * Create a new item listing.
     * Seller identity is authoritatively derived from the authenticated principal.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String headerStudentId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to create an auction listing."));
        }
        request.setSellerId(principal.getStudentId() != null ? principal.getStudentId() : principal.getEmail());
        request.setSellerName(principal.getFullName());
        request.setSellerEmail(principal.getEmail());
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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to update listings."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to cancel listings."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        ListingResponse cancelled = listingService.cancelListing(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Listing cancelled successfully", cancelled));
    }

    /**
     * Support HTTP DELETE for cancelling listings as well.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelListingDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to cancel listings."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        ListingResponse cancelled = listingService.cancelListing(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Listing cancelled successfully", cancelled));
    }

    /**
     * Place a bid on an active auction.
     * Bidder identity is strictly derived from the authenticated principal.
     */
    @PostMapping("/{id}/bids")
    public ResponseEntity<ApiResponse<ListingResponse>> placeBid(
            @PathVariable Long id,
            @Valid @RequestBody com.campusbid.backend.dto.PlaceBidRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "bidderId", required = false) String paramBidderId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Please sign in to place a bid."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramBidderId);
        request.setBidderId(effectiveStudentId);
        request.setBidderName(principal.getFullName());
        ListingResponse updated = listingService.placeBid(id, request, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Bid of $%.2f placed successfully!", request.getAmount()),
                updated
        ));
    }

    /**
     * View complete public bid history for an auction.
     */
    @GetMapping("/{id}/bids")
    public ResponseEntity<ApiResponse<List<com.campusbid.backend.model.BidRecord>>> getBidHistory(@PathVariable Long id) {
        List<com.campusbid.backend.model.BidRecord> bids = listingService.getBidHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Bid history retrieved", bids));
    }

    /**
     * View listings created by a specific student.
     */
    @GetMapping("/my-listings")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getMyListings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to access personal listings."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        List<ListingResponse> listings = listingService.getListingsBySeller(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Seller listings retrieved", listings));
    }

    /**
     * View closed listings awaiting seller decision for the authenticated student.
     */
    @GetMapping("/pending-decisions")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getPendingDecisions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to review pending decisions."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        List<ListingResponse> pending = listingService.getPendingDecisions(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Pending decisions retrieved", pending));
    }

    /**
     * View listings won by the authenticated student.
     */
    @GetMapping("/won")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> getWonListings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "studentId", required = false) String paramStudentId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to view won lots."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramStudentId);
        List<ListingResponse> won = listingService.getWonListings(effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Won listings retrieved", won));
    }

    /**
     * Seller decision: APPROVE or REJECT the winning bid on a closed auction.
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<ListingResponse>> processDecision(
            @PathVariable Long id,
            @Valid @RequestBody com.campusbid.backend.dto.SellerDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to manage auction decisions."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        ListingResponse updated = listingService.processSellerDecision(
                id,
                request.getDecision(),
                request.getNotes(),
                effectiveStudentId
        );
        String msg = "APPROVE".equalsIgnoreCase(request.getDecision())
                ? "Winning bid approved! Item reserved for buyer."
                : "Winning bid rejected.";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    /**
     * Convenience shortcut to approve winning bid.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ListingResponse>> approveWinningBid(
            @PathVariable Long id,
            @RequestBody(required = false) com.campusbid.backend.dto.SellerDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to manage auction decisions."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        String notes = request != null ? request.getNotes() : null;
        ListingResponse updated = listingService.processSellerDecision(id, "APPROVE", notes, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Winning bid approved! Item reserved for buyer.", updated));
    }

    /**
     * Convenience shortcut to reject winning bid.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ListingResponse>> rejectWinningBid(
            @PathVariable Long id,
            @RequestBody(required = false) com.campusbid.backend.dto.SellerDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to manage auction decisions."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        String notes = request != null ? request.getNotes() : null;
        ListingResponse updated = listingService.processSellerDecision(id, "REJECT", notes, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Winning bid rejected.", updated));
    }

    /**
     * Confirm campus handover by seller or winning bidder.
     */
    @PostMapping("/{id}/confirm-handover")
    public ResponseEntity<ApiResponse<ListingResponse>> confirmHandover(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "studentId", required = false) String paramStudentId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to confirm handover."));
        }
        String effectiveStudentId = resolveStudentId(principal, studentId, paramStudentId);
        ListingResponse updated = listingService.confirmHandover(id, effectiveStudentId);
        String msg = updated.getStatus() == com.campusbid.backend.model.ListingStatus.COMPLETED
                ? "🎉 Campus handover confirmed by both parties! Sale COMPLETED."
                : "Handover confirmation recorded. Awaiting counterpart confirmation.";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    /**
     * Finalize campus handover directly in one step.
     */
    @PostMapping("/{id}/finalize-handover")
    public ResponseEntity<ApiResponse<ListingResponse>> finalizeHandover(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "studentId", required = false) String paramStudentId
    ) {
        String effectiveStudentId = resolveStudentId(principal, studentId, paramStudentId);
        ListingResponse updated = listingService.finalizeHandover(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("🎉 Campus handover finalized! Sale COMPLETED.", updated));
    }

    /**
     * Cancel approved sale or record failed pickup.
     */
    @PostMapping("/{id}/cancel-sale")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelSale(
            @PathVariable Long id,
            @RequestBody(required = false) com.campusbid.backend.dto.CancelSaleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "studentId", required = false) String paramStudentId
    ) {
        String effectiveStudentId = resolveStudentId(principal, studentId, paramStudentId);
        String reason = request != null ? request.getReason() : "Sale cancelled / failed pickup";
        ListingResponse updated = listingService.cancelSale(id, reason, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Sale cancelled. Listing updated.", updated));
    }

    /**
     * Trigger manual close of an active auction.
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<ListingResponse>> closeAuction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId,
            @RequestParam(value = "sellerId", required = false) String paramSellerId
    ) {
        String effectiveStudentId = resolveStudentId(principal, studentId, paramSellerId);
        ListingResponse updated = listingService.closeAuction(id, effectiveStudentId);
        return ResponseEntity.ok(ApiResponse.success("Auction closed successfully.", updated));
    }

    /**
     * Batch process closing for all expired active auctions.
     */
    @PostMapping("/close-expired")
    public ResponseEntity<ApiResponse<Integer>> processExpiredAuctions() {
        int count = listingService.processExpiredAuctions();
        return ResponseEntity.ok(ApiResponse.success("Expired auctions processed: " + count, count));
    }
}
