package com.campusbid.backend.service;

import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.PlaceBidRequest;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.exception.ResourceNotFoundException;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.BidRecord;
import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.model.NotificationType;
import com.campusbid.backend.repository.ListingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final NotificationService notificationService;
    private final WantedItemService wantedItemService;

    public ListingService(ListingRepository listingRepository, NotificationService notificationService) {
        this(listingRepository, notificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ListingService(ListingRepository listingRepository,
                          NotificationService notificationService,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) WantedItemService wantedItemService) {
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
        this.wantedItemService = wantedItemService;
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

        // Smart alerts: automatically scan and match active wanted items
        if (wantedItemService != null) {
            wantedItemService.checkAndAlertMatches(saved);
        }

        return ListingResponse.fromEntity(saved);
    }

    /**
     * Search and filter active auctions with backend closing time enforcement.
     */
    public List<ListingResponse> getActiveListings(
            String category,
            ItemCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean closingSoon,
            String keyword,
            String sortBy
    ) {
        LocalDateTime now = LocalDateTime.now();
        // Automatically close any expired active auctions and select winners
        processExpiredAuctions();

        Sort sort = determineSort(sortBy);
        LocalDateTime closingSoonThreshold = (closingSoon != null && closingSoon)
                ? now.plusHours(24)
                : null;

        String effectiveCategory = (category != null && !category.isBlank() && !category.equalsIgnoreCase("All"))
                ? category.trim()
                : null;
        String effectiveKeyword = (keyword != null && !keyword.isBlank())
                ? keyword.trim()
                : null;

        List<Listing> listings = listingRepository.searchWithFilters(
                ListingStatus.ACTIVE,
                effectiveCategory,
                condition,
                minPrice,
                maxPrice,
                closingSoonThreshold,
                now,
                effectiveKeyword,
                sort
        );

        return listings.stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search and filter all listings (including closed, approved, and cancelled).
     */
    public List<ListingResponse> getAllListings(
            ListingStatus status,
            String category,
            ItemCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean closingSoon,
            String keyword,
            String sortBy
    ) {
        LocalDateTime now = LocalDateTime.now();
        processExpiredAuctions();

        Sort sort = determineSort(sortBy);
        LocalDateTime closingSoonThreshold = (closingSoon != null && closingSoon)
                ? now.plusHours(24)
                : null;

        String effectiveCategory = (category != null && !category.isBlank() && !category.equalsIgnoreCase("All"))
                ? category.trim()
                : null;
        String effectiveKeyword = (keyword != null && !keyword.isBlank())
                ? keyword.trim()
                : null;

        List<Listing> listings = listingRepository.searchWithFilters(
                status,
                effectiveCategory,
                condition,
                minPrice,
                maxPrice,
                closingSoonThreshold,
                now,
                effectiveKeyword,
                sort
        );

        return listings.stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        // Enforce closing check on individual lookup
        if (listing.getStatus() == ListingStatus.ACTIVE && LocalDateTime.now().isAfter(listing.getClosingTime())) {
            closeSingleAuction(listing, LocalDateTime.now());
            listing = listingRepository.findById(id).orElse(listing);
        }

        return ListingResponse.fromEntity(listing);
    }

    public ListingResponse updateListing(Long id, UpdateListingRequest request, String requesterStudentId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + id));

        // Ownership Verification
        validateSellerOwnership(listing, requesterStudentId);

        // Cannot edit if already cancelled
        if (listing.getStatus() == ListingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled listings cannot be edited.");
        }

        // Backend enforcement: cannot edit an auction that has already closed
        if (listing.getStatus() != ListingStatus.ACTIVE || LocalDateTime.now().isAfter(listing.getClosingTime())) {
            if (listing.getStatus() == ListingStatus.ACTIVE) {
                closeSingleAuction(listing, LocalDateTime.now());
            }
            throw new IllegalArgumentException("Cannot edit an auction that has already closed.");
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

        // Backend enforcement: cannot cancel an auction that has already closed
        if (listing.getStatus() != ListingStatus.ACTIVE || LocalDateTime.now().isAfter(listing.getClosingTime())) {
            if (listing.getStatus() == ListingStatus.ACTIVE) {
                closeSingleAuction(listing, LocalDateTime.now());
            }
            throw new IllegalArgumentException("Cannot cancel an auction that has already closed.");
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

    /**
     * Place a bid on an active listing with concurrency control (pessimistic row lock),
     * strict validation against minimum permitted next bid, seller self-bidding prevention,
     * deadline enforcement, and recording bid history.
     */
    public ListingResponse placeBid(Long listingId, PlaceBidRequest request, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required. Please specify a student identifier (X-Student-Id).");
        }
        String bidderId = authenticatedStudentId.trim();

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Bid amount must be a positive number.");
        }

        // Acquire exclusive pessimistic write lock to serialize concurrent bids on this lot
        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        LocalDateTime now = LocalDateTime.now();

        // Enforce auction status: cannot bid on cancelled listing
        if (listing.getStatus() == ListingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot place a bid on a cancelled auction.");
        }

        // Enforce auction status: cannot bid on ended/closed/non-active listing
        if (listing.getStatus() != ListingStatus.ACTIVE || (listing.getClosingTime() != null && now.isAfter(listing.getClosingTime()))) {
            if (listing.getStatus() == ListingStatus.ACTIVE) {
                closeSingleAuction(listing, now);
            }
            throw new IllegalArgumentException("Cannot place a bid on an auction that has already closed.");
        }

        // Prevent seller from bidding on their own listing
        if (listing.getSellerId().trim().equalsIgnoreCase(bidderId)) {
            throw new IllegalArgumentException("Sellers are prohibited from bidding on their own lots and listings.");
        }

        // Calculate minimum permitted bid amount
        boolean isFirstBid = listing.getBidHistory() == null || listing.getBidHistory().isEmpty();
        BigDecimal minPermittedBid = isFirstBid
                ? listing.getStartingPrice()
                : listing.getCurrentPrice().add(listing.getMinBidIncrement());

        if (request.getAmount().compareTo(minPermittedBid) < 0) {
            if (isFirstBid) {
                throw new IllegalArgumentException(String.format(
                        "Bid must be at least the starting price of $%.2f. Submitted bid: $%.2f",
                        minPermittedBid, request.getAmount()
                ));
            } else {
                throw new IllegalArgumentException(String.format(
                        "Bid must be at least $%.2f (Current leading bid: $%.2f + Minimum increment: $%.2f). Submitted bid: $%.2f",
                        minPermittedBid, listing.getCurrentPrice(), listing.getMinBidIncrement(), request.getAmount()
                ));
            }
        }

        String effectiveBidderName = request.getBidderName() != null && !request.getBidderName().isBlank()
                ? request.getBidderName().trim()
                : bidderId;

        // Find previous leading bidder before recording new bid
        String previousLeadingBidderId = null;
        String previousLeadingBidderName = null;
        if (listing.getBidHistory() != null && !listing.getBidHistory().isEmpty()) {
            BidRecord topBid = listing.getBidHistory().stream()
                    .max((a, b) -> a.getAmount().compareTo(b.getAmount()))
                    .orElse(null);
            if (topBid != null) {
                previousLeadingBidderId = topBid.getBidderId();
                previousLeadingBidderName = topBid.getBidderName();
            }
        }

        // Record accepted bid with server-generated timestamp
        BidRecord bidRecord = new BidRecord(bidderId.trim(), effectiveBidderName, request.getAmount(), now);
        listing.getBidHistory().add(bidRecord);

        // Update current leading bid
        listing.setCurrentPrice(request.getAmount());
        listing.setUpdatedAt(now);

        Listing saved = listingRepository.save(listing);

        // 1. Notify current bidder that their bid was accepted as the leading bid
        notificationService.sendNotification(
                bidderId,
                effectiveBidderName,
                "Bid Accepted: Leading Bid on LOT #" + String.format("%03d", listing.getId()),
                String.format("Your bid of $%.2f on \"%s\" has been accepted and is currently the leading bid!",
                        request.getAmount(), listing.getTitle()),
                NotificationType.BID_ACCEPTED,
                listing.getId(),
                listing.getTitle()
        );

        // 2. Notify seller that a new bid was placed on their item
        notificationService.sendNotification(
                listing.getSellerId(),
                listing.getSellerName(),
                "New Bid Placed on LOT #" + String.format("%03d", listing.getId()),
                String.format("%s placed a new leading bid of $%.2f on your lot \"%s\".",
                        effectiveBidderName, request.getAmount(), listing.getTitle()),
                NotificationType.BID_RECEIVED,
                listing.getId(),
                listing.getTitle()
        );

        // 3. Notify previous leading bidder that they were outbid
        if (previousLeadingBidderId != null && !previousLeadingBidderId.equalsIgnoreCase(bidderId)) {
            notificationService.sendNotification(
                    previousLeadingBidderId,
                    previousLeadingBidderName,
                    "Outbid Alert on LOT #" + String.format("%03d", listing.getId()),
                    String.format("You have been outbid on \"%s\"! %s placed a higher leading bid of $%.2f. Place a new bid before closing to win this lot.",
                            listing.getTitle(), effectiveBidderName, request.getAmount()),
                    NotificationType.OUTBID,
                    listing.getId(),
                    listing.getTitle()
            );
        }

        return ListingResponse.fromEntity(saved);
    }

    /**
     * Retrieve complete public bid history for an auction.
     */
    @Transactional(readOnly = true)
    public List<BidRecord> getBidHistory(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));
        return new java.util.ArrayList<>(listing.getBidHistory());
    }

    /**
     * Closes an auction, identifies winning bidder (highest amount),
     * and sets status to AWAITING_SELLER_APPROVAL or UNSOLD.
     */
    public void closeSingleAuction(Listing listing, LocalDateTime now) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            return;
        }

        boolean hasBids = listing.getBidHistory() != null && !listing.getBidHistory().isEmpty();
        if (hasBids) {
            // Identify highest valid bidder
            BidRecord winner = listing.getBidHistory().stream()
                    .max((a, b) -> {
                        int cmp = a.getAmount().compareTo(b.getAmount());
                        if (cmp != 0) return cmp;
                        return b.getBidTime().compareTo(a.getBidTime());
                    })
                    .orElse(listing.getBidHistory().get(0));

            listing.setWinningBidderId(winner.getBidderId());
            listing.setWinningBidderName(winner.getBidderName());
            listing.setWinningBidAmount(winner.getAmount());
            listing.setWinningBidTime(winner.getBidTime());
            listing.setStatus(ListingStatus.AWAITING_SELLER_APPROVAL);

            listing.setUpdatedAt(now);
            listingRepository.save(listing);

            // 1. Notify winning buyer that they won the auction (deduplicated)
            notificationService.sendNotificationIfNotExists(
                    winner.getBidderId(),
                    winner.getBidderName(),
                    "Congratulations! You Won LOT #" + String.format("%03d", listing.getId()),
                    String.format("You placed the winning bid of $%.2f for \"%s\"! The auction has closed and is awaiting seller approval.",
                            winner.getAmount(), listing.getTitle()),
                    NotificationType.AUCTION_WON,
                    listing.getId(),
                    listing.getTitle()
            );

            // 2. Notify seller that a winning bid is waiting for their approval (deduplicated)
            notificationService.sendNotificationIfNotExists(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Action Needed: Winning Bid Awaiting Approval on LOT #" + String.format("%03d", listing.getId()),
                    String.format("Your auction for \"%s\" has concluded! The highest bid of $%.2f was placed by %s. Please review and approve or reject this bid.",
                            listing.getTitle(), winner.getAmount(), winner.getBidderName()),
                    NotificationType.AWAITING_APPROVAL,
                    listing.getId(),
                    listing.getTitle()
            );

            // 3. Notify seller that auction closed (deduplicated)
            notificationService.sendNotificationIfNotExists(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Auction Closed: LOT #" + String.format("%03d", listing.getId()),
                    String.format("Auction closed for \"%s\" with %d bids. Highest bid: $%.2f.",
                            listing.getTitle(), listing.getBidHistory().size(), winner.getAmount()),
                    NotificationType.AUCTION_CLOSED,
                    listing.getId(),
                    listing.getTitle()
            );
        } else {
            listing.setStatus(ListingStatus.UNSOLD);
            listing.setUpdatedAt(now);
            listingRepository.save(listing);

            // Notify seller that auction closed with no bids (deduplicated)
            notificationService.sendNotificationIfNotExists(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Auction Closed (Unsold): LOT #" + String.format("%03d", listing.getId()),
                    String.format("Your auction for \"%s\" reached its deadline with no bids and has been marked as UNSOLD.",
                            listing.getTitle()),
                    NotificationType.AUCTION_CLOSED,
                    listing.getId(),
                    listing.getTitle()
            );
        }
    }

    /**
     * Batch process all expired active auctions with winner identification and status transition.
     */
    public int processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Listing> expired = listingRepository.findExpiredActiveListings(now);
        int count = 0;
        for (Listing listing : expired) {
            closeSingleAuction(listing, now);
            count++;
        }
        return count;
    }

    /**
     * Scheduled background task: runs every 15 seconds to automatically close expired auctions.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 15000)
    public void scheduledAuctionClosing() {
        processExpiredAuctions();
    }

    /**
     * Manually close an active auction by seller before deadline.
     */
    public ListingResponse closeAuction(Long listingId, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required.");
        }
        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        validateSellerOwnership(listing, authenticatedStudentId);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing is not active. Current status: " + listing.getStatus());
        }

        closeSingleAuction(listing, LocalDateTime.now());
        Listing saved = listingRepository.findById(listingId).orElse(listing);
        return ListingResponse.fromEntity(saved);
    }

    /**
     * Process seller's decision to APPROVE or REJECT the winning bid on an auction.
     */
    public ListingResponse processSellerDecision(Long listingId, String decision, String notes, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required. Please specify a student identifier via X-Student-Id header.");
        }

        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        validateSellerOwnership(listing, authenticatedStudentId);

        // If auction is still ACTIVE but closing time passed, close it first
        if (listing.getStatus() == ListingStatus.ACTIVE) {
            if (listing.getClosingTime() != null && LocalDateTime.now().isAfter(listing.getClosingTime())) {
                closeSingleAuction(listing, LocalDateTime.now());
            } else {
                throw new IllegalArgumentException("Cannot make a decision while auction is still actively running.");
            }
        }

        if (listing.getStatus() == ListingStatus.UNSOLD) {
            throw new IllegalArgumentException("Auction concluded with no bids (UNSOLD). No winning bid to approve or reject.");
        }

        if (listing.getStatus() == ListingStatus.APPROVED) {
            throw new IllegalArgumentException("The winning bid has already been approved.");
        }

        if (listing.getStatus() == ListingStatus.REJECTED) {
            throw new IllegalArgumentException("The winning bid has already been rejected.");
        }

        if (listing.getStatus() == ListingStatus.COMPLETED) {
            throw new IllegalArgumentException("This sale has already been completed.");
        }

        if (listing.getStatus() != ListingStatus.AWAITING_SELLER_APPROVAL && listing.getStatus() != ListingStatus.ENDED && listing.getStatus() != ListingStatus.CLOSED) {
            throw new IllegalArgumentException("Listing is not awaiting seller approval. Current status: " + listing.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        if ("APPROVE".equalsIgnoreCase(decision.trim())) {
            listing.setStatus(ListingStatus.APPROVED);
            listing.setSellerDecision("APPROVED");
            listing.setSellerDecisionTime(now);
            String finalNotes = notes != null && !notes.isBlank() ? notes.trim() : "Approved by seller. Item reserved for winning bidder.";
            listing.setSellerDecisionNotes(finalNotes);

            // Notify winning buyer that their bid was approved and item is reserved
            notificationService.sendNotification(
                    listing.getWinningBidderId(),
                    listing.getWinningBidderName(),
                    "Bid Approved! LOT #" + String.format("%03d", listing.getId()) + " Reserved For You",
                    String.format("Great news! The seller approved your winning bid of $%.2f for \"%s\". The item is now reserved. Note: %s",
                            listing.getWinningBidAmount(), listing.getTitle(), finalNotes),
                    NotificationType.BID_APPROVED,
                    listing.getId(),
                    listing.getTitle()
            );
        } else if ("REJECT".equalsIgnoreCase(decision.trim())) {
            listing.setStatus(ListingStatus.REJECTED);
            listing.setSellerDecision("REJECTED");
            listing.setSellerDecisionTime(now);
            String finalNotes = notes != null && !notes.isBlank() ? notes.trim() : "Winning bid declined by seller.";
            listing.setSellerDecisionNotes(finalNotes);

            // Notify winning buyer that their bid was declined
            notificationService.sendNotification(
                    listing.getWinningBidderId(),
                    listing.getWinningBidderName(),
                    "Bid Declined on LOT #" + String.format("%03d", listing.getId()),
                    String.format("The seller has declined the winning bid of $%.2f for \"%s\". Reason: %s",
                            listing.getWinningBidAmount(), listing.getTitle(), finalNotes),
                    NotificationType.BID_REJECTED,
                    listing.getId(),
                    listing.getTitle()
            );
        } else {
            throw new IllegalArgumentException("Invalid decision. Must be APPROVE or REJECT.");
        }

        listing.setUpdatedAt(now);
        Listing saved = listingRepository.save(listing);
        return ListingResponse.fromEntity(saved);
    }

    /**
     * Confirm campus handover by seller or winning bidder.
     * Transitions sale to COMPLETED once confirmed.
     */
    public ListingResponse confirmHandover(Long listingId, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required. Please specify a student identifier via X-Student-Id header.");
        }

        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        if (listing.getStatus() == ListingStatus.COMPLETED) {
            throw new IllegalArgumentException("Campus handover has already been completed and confirmed.");
        }

        if (listing.getStatus() != ListingStatus.APPROVED) {
            throw new IllegalArgumentException("Handover can only be confirmed for approved sales. Current status: " + listing.getStatus());
        }

        String studentId = authenticatedStudentId.trim();
        boolean isSeller = listing.getSellerId().trim().equalsIgnoreCase(studentId);
        boolean isWinner = listing.getWinningBidderId() != null && listing.getWinningBidderId().trim().equalsIgnoreCase(studentId);

        if (!isSeller && !isWinner) {
            throw new UnauthorizedException("Only the seller or the winning bidder can confirm campus handover.");
        }

        if (isSeller) {
            listing.setHandoverConfirmedBySeller(true);
            // Notify winning buyer that seller confirmed handover
            notificationService.sendNotification(
                    listing.getWinningBidderId(),
                    listing.getWinningBidderName(),
                    "Handover Update on LOT #" + String.format("%03d", listing.getId()),
                    String.format("The seller (%s) confirmed campus handover for \"%s\". Please confirm once you receive your item to complete the sale.",
                            listing.getSellerName(), listing.getTitle()),
                    NotificationType.HANDOVER_CONFIRMED,
                    listing.getId(),
                    listing.getTitle()
            );
        }
        if (isWinner) {
            listing.setHandoverConfirmedByWinner(true);
            // Notify seller that winning buyer confirmed receiving item
            notificationService.sendNotification(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Handover Update on LOT #" + String.format("%03d", listing.getId()),
                    String.format("The winning buyer (%s) confirmed receiving the item for \"%s\". Please confirm from your side if not yet done.",
                            listing.getWinningBidderName(), listing.getTitle()),
                    NotificationType.HANDOVER_CONFIRMED,
                    listing.getId(),
                    listing.getTitle()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        // If both confirmed, complete sale
        if (Boolean.TRUE.equals(listing.getHandoverConfirmedBySeller()) && Boolean.TRUE.equals(listing.getHandoverConfirmedByWinner())) {
            listing.setStatus(ListingStatus.COMPLETED);
            listing.setHandoverTime(now);

            // Notify seller of completion
            notificationService.sendNotificationIfNotExists(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Sale Completed! LOT #" + String.format("%03d", listing.getId()),
                    String.format("Campus exchange confirmed by both parties! The sale of \"%s\" to %s for $%.2f is complete.",
                            listing.getTitle(), listing.getWinningBidderName(), listing.getWinningBidAmount()),
                    NotificationType.SALE_COMPLETED,
                    listing.getId(),
                    listing.getTitle()
            );

            // Notify buyer of completion
            notificationService.sendNotificationIfNotExists(
                    listing.getWinningBidderId(),
                    listing.getWinningBidderName(),
                    "Sale Completed! LOT #" + String.format("%03d", listing.getId()),
                    String.format("Campus handover confirmed! Your purchase of \"%s\" for $%.2f is officially completed. Enjoy your item!",
                            listing.getTitle(), listing.getWinningBidAmount()),
                    NotificationType.SALE_COMPLETED,
                    listing.getId(),
                    listing.getTitle()
            );
        }

        listing.setUpdatedAt(now);
        Listing saved = listingRepository.save(listing);
        return ListingResponse.fromEntity(saved);
    }

    /**
     * Mark handover completed directly by seller or winner.
     */
    public ListingResponse finalizeHandover(Long listingId, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required.");
        }

        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        String studentId = authenticatedStudentId.trim();
        boolean isSeller = listing.getSellerId().trim().equalsIgnoreCase(studentId);
        boolean isWinner = listing.getWinningBidderId() != null && listing.getWinningBidderId().trim().equalsIgnoreCase(studentId);

        if (!isSeller && !isWinner) {
            throw new UnauthorizedException("Only the seller or the winning bidder can confirm campus handover.");
        }

        if (listing.getStatus() != ListingStatus.APPROVED) {
            throw new IllegalArgumentException("Handover can only be confirmed for approved sales. Current status: " + listing.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        listing.setHandoverConfirmedBySeller(true);
        listing.setHandoverConfirmedByWinner(true);
        listing.setStatus(ListingStatus.COMPLETED);
        listing.setHandoverTime(now);
        listing.setUpdatedAt(now);

        Listing saved = listingRepository.save(listing);

        // Notify seller of completion
        notificationService.sendNotificationIfNotExists(
                listing.getSellerId(),
                listing.getSellerName(),
                "Sale Completed! LOT #" + String.format("%03d", listing.getId()),
                String.format("Campus exchange confirmed by both parties! The sale of \"%s\" to %s for $%.2f is complete.",
                        listing.getTitle(), listing.getWinningBidderName(), listing.getWinningBidAmount()),
                NotificationType.SALE_COMPLETED,
                listing.getId(),
                listing.getTitle()
        );

        // Notify buyer of completion
        notificationService.sendNotificationIfNotExists(
                listing.getWinningBidderId(),
                listing.getWinningBidderName(),
                "Sale Completed! LOT #" + String.format("%03d", listing.getId()),
                String.format("Campus handover confirmed! Your purchase of \"%s\" for $%.2f is officially completed. Enjoy your item!",
                        listing.getTitle(), listing.getWinningBidAmount()),
                NotificationType.SALE_COMPLETED,
                listing.getId(),
                listing.getTitle()
        );

        return ListingResponse.fromEntity(saved);
    }

    /**
     * Cancel approved sale or record failed pickup without deleting auction or bid history.
     */
    public ListingResponse cancelSale(Long listingId, String reason, String authenticatedStudentId) {
        if (authenticatedStudentId == null || authenticatedStudentId.isBlank()) {
            throw new UnauthorizedException("Authentication required.");
        }

        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + listingId));

        String studentId = authenticatedStudentId.trim();
        boolean isSeller = listing.getSellerId().trim().equalsIgnoreCase(studentId);
        boolean isWinner = listing.getWinningBidderId() != null && listing.getWinningBidderId().trim().equalsIgnoreCase(studentId);

        if (!isSeller && !isWinner) {
            throw new UnauthorizedException("Only the seller or winning bidder can cancel an approved sale.");
        }

        if (listing.getStatus() == ListingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a sale that has already been completed with confirmed handover.");
        }

        LocalDateTime now = LocalDateTime.now();
        listing.setStatus(ListingStatus.CANCELLED);
        String finalReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Sale cancelled / failed pickup";
        listing.setSellerDecisionNotes(finalReason);
        listing.setUpdatedAt(now);

        Listing saved = listingRepository.save(listing);

        // Notify the other party of cancellation
        if (isSeller && listing.getWinningBidderId() != null) {
            notificationService.sendNotification(
                    listing.getWinningBidderId(),
                    listing.getWinningBidderName(),
                    "Sale Cancelled for LOT #" + String.format("%03d", listing.getId()),
                    String.format("The sale for \"%s\" was cancelled by the seller. Reason: %s",
                            listing.getTitle(), finalReason),
                    NotificationType.SALE_CANCELLED,
                    listing.getId(),
                    listing.getTitle()
            );
        } else if (isWinner) {
            notificationService.sendNotification(
                    listing.getSellerId(),
                    listing.getSellerName(),
                    "Sale Cancelled for LOT #" + String.format("%03d", listing.getId()),
                    String.format("The winning buyer cancelled the purchase for \"%s\". Reason: %s",
                            listing.getTitle(), finalReason),
                    NotificationType.SALE_CANCELLED,
                    listing.getId(),
                    listing.getTitle()
            );
        }

        return ListingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getPendingDecisions(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return listingRepository.findBySellerIdAndStatusOrderByClosingTimeDesc(sellerId.trim(), ListingStatus.AWAITING_SELLER_APPROVAL)
                .stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getWonListings(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return listingRepository.findByWinningBidderIdOrderByClosingTimeDesc(studentId.trim())
                .stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateSellerOwnership(Listing listing, String requesterStudentId) {
        if (requesterStudentId == null || requesterStudentId.trim().isEmpty()) {
            throw new UnauthorizedException("Access Denied: Missing student identifier header (X-Student-Id). Please specify who you are.");
        }
        if (!listing.getSellerId().trim().equalsIgnoreCase(requesterStudentId.trim())) {
            throw new UnauthorizedException("Access Denied: You cannot modify or manage another student's listing. This item was listed by: " + listing.getSellerId());
        }
    }

    private Sort determineSort(String sortBy) {
        if ("closingSoon".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "closingTime");
        } else if ("priceLow".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "currentPrice");
        } else if ("priceHigh".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "currentPrice");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
