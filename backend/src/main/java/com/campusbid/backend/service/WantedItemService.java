package com.campusbid.backend.service;

import com.campusbid.backend.dto.WantedItemRequest;
import com.campusbid.backend.dto.WantedItemResponse;
import com.campusbid.backend.dto.WantedMatchDto;
import com.campusbid.backend.exception.ResourceNotFoundException;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.*;
import com.campusbid.backend.repository.ListingRepository;
import com.campusbid.backend.repository.WantedItemMatchRepository;
import com.campusbid.backend.repository.WantedItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class WantedItemService {

    private static final Logger log = LoggerFactory.getLogger(WantedItemService.class);

    private final WantedItemRepository wantedItemRepository;
    private final WantedItemMatchRepository wantedItemMatchRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public WantedItemService(WantedItemRepository wantedItemRepository,
                             WantedItemMatchRepository wantedItemMatchRepository,
                             ListingRepository listingRepository,
                             NotificationService notificationService) {
        this.wantedItemRepository = wantedItemRepository;
        this.wantedItemMatchRepository = wantedItemMatchRepository;
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Create a new wanted item alert request for the authenticated student.
     */
    /**
     * Create a new wanted item alert request for the authenticated student.
     */
    public WantedItemResponse createWantedItem(WantedItemRequest request, String studentId, String studentName, String studentEmail) {
        if (request.getMaxBudget() == null || request.getMaxBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum budget must be greater than zero.");
        }
        if (request.getItemName() == null || request.getItemName().trim().isBlank()) {
            throw new IllegalArgumentException("Item name or keywords are required.");
        }

        String effectiveId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : (studentEmail != null ? studentEmail.trim() : "student");
        String effectiveName = (studentName != null && !studentName.isBlank())
                ? studentName.trim()
                : effectiveId;
        String effectiveEmail = (studentEmail != null && !studentEmail.isBlank())
                ? studentEmail.trim()
                : null;

        WantedItem item = new WantedItem(
                effectiveId,
                effectiveName,
                effectiveEmail,
                request.getItemName().trim(),
                request.getCategory() != null ? request.getCategory().trim() : null,
                request.getMaxBudget(),
                request.getPreferredCondition(),
                request.getAdditionalRequirements() != null ? request.getAdditionalRequirements().trim() : null
        );

        WantedItem saved = wantedItemRepository.save(item);

        // Check against active listings already in the database
        scanExistingActiveListingsForMatches(saved);

        List<WantedMatchDto> matches = wantedItemMatchRepository.findByWantedItemIdOrderByMatchedAtDesc(saved.getId())
                .stream().filter(Objects::nonNull).map(WantedMatchDto::fromEntity).filter(Objects::nonNull).collect(Collectors.toList());

        return WantedItemResponse.fromEntity(saved, matches);
    }

    /**
     * Get all wanted item requests for the given student (by studentId and/or studentEmail).
     */
    @Transactional(readOnly = true)
    public List<WantedItemResponse> getStudentWantedItems(String studentId, String studentEmail) {
        String sId = (studentId != null && !studentId.isBlank()) ? studentId.trim() : null;
        String sEmail = (studentEmail != null && !studentEmail.isBlank()) ? studentEmail.trim() : null;

        if (sId == null && sEmail == null) {
            return Collections.emptyList();
        }

        List<WantedItem> items;
        if (sId != null && sEmail != null) {
            items = wantedItemRepository.findByStudentIdOrStudentEmailOrderByCreatedAtDesc(sId, sEmail);
        } else if (sId != null) {
            items = wantedItemRepository.findByStudentIdOrderByCreatedAtDesc(sId);
        } else {
            items = wantedItemRepository.findByStudentEmailOrderByCreatedAtDesc(sEmail);
        }

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> itemIds = items.stream().map(WantedItem::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<WantedItemMatch> allMatches = itemIds.isEmpty()
                ? Collections.emptyList()
                : wantedItemMatchRepository.findByWantedItemIdInOrderByMatchedAtDesc(itemIds);

        Map<Long, List<WantedMatchDto>> matchMap = allMatches.stream()
                .filter(Objects::nonNull)
                .map(WantedMatchDto::fromEntity)
                .filter(Objects::nonNull)
                .filter(m -> m.getWantedItemId() != null)
                .collect(Collectors.groupingBy(WantedMatchDto::getWantedItemId));

        return items.stream()
                .map(it -> WantedItemResponse.fromEntity(it, matchMap.getOrDefault(it.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WantedItemResponse> getStudentWantedItems(String studentId) {
        return getStudentWantedItems(studentId, null);
    }

    private boolean isOwner(WantedItem item, String studentId, String studentEmail) {
        if (item == null) return false;
        String sId = (studentId != null && !studentId.isBlank()) ? studentId.trim() : null;
        String sEmail = (studentEmail != null && !studentEmail.isBlank()) ? studentEmail.trim() : null;

        if (sId != null) {
            if (item.getStudentId() != null && sId.equalsIgnoreCase(item.getStudentId().trim())) return true;
            if (item.getStudentEmail() != null && sId.equalsIgnoreCase(item.getStudentEmail().trim())) return true;
        }
        if (sEmail != null) {
            if (item.getStudentEmail() != null && sEmail.equalsIgnoreCase(item.getStudentEmail().trim())) return true;
            if (item.getStudentId() != null && sEmail.equalsIgnoreCase(item.getStudentId().trim())) return true;
        }
        return false;
    }

    /**
     * Get single wanted item by ID, enforcing ownership.
     */
    @Transactional(readOnly = true)
    public WantedItemResponse getWantedItemById(Long id, String studentId, String studentEmail) {
        WantedItem item = wantedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wanted item request not found with ID: " + id));

        if (!isOwner(item, studentId, studentEmail)) {
            throw new UnauthorizedException("You are not authorized to view another student's wanted item request.");
        }

        List<WantedMatchDto> matches = wantedItemMatchRepository.findByWantedItemIdOrderByMatchedAtDesc(item.getId())
                .stream().filter(Objects::nonNull).map(WantedMatchDto::fromEntity).filter(Objects::nonNull).collect(Collectors.toList());

        return WantedItemResponse.fromEntity(item, matches);
    }

    @Transactional(readOnly = true)
    public WantedItemResponse getWantedItemById(Long id, String studentId) {
        return getWantedItemById(id, studentId, null);
    }

    /**
     * Update an existing wanted item request, enforcing ownership.
     */
    public WantedItemResponse updateWantedItem(Long id, WantedItemRequest request, String studentId, String studentEmail) {
        WantedItem item = wantedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wanted item request not found with ID: " + id));

        if (!isOwner(item, studentId, studentEmail)) {
            throw new UnauthorizedException("You are not authorized to update another student's wanted item request.");
        }

        if (request.getMaxBudget() == null || request.getMaxBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum budget must be greater than zero.");
        }
        if (request.getItemName() == null || request.getItemName().trim().isBlank()) {
            throw new IllegalArgumentException("Item name or keywords are required.");
        }

        item.setItemName(request.getItemName().trim());
        item.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        item.setMaxBudget(request.getMaxBudget());
        item.setPreferredCondition(request.getPreferredCondition());
        item.setAdditionalRequirements(request.getAdditionalRequirements() != null ? request.getAdditionalRequirements().trim() : null);

        WantedItem updated = wantedItemRepository.save(item);

        if (updated.getStatus() == WantedItemStatus.ACTIVE) {
            scanExistingActiveListingsForMatches(updated);
        }

        List<WantedMatchDto> matches = wantedItemMatchRepository.findByWantedItemIdOrderByMatchedAtDesc(updated.getId())
                .stream().filter(Objects::nonNull).map(WantedMatchDto::fromEntity).filter(Objects::nonNull).collect(Collectors.toList());

        return WantedItemResponse.fromEntity(updated, matches);
    }

    public WantedItemResponse updateWantedItem(Long id, WantedItemRequest request, String studentId) {
        return updateWantedItem(id, request, studentId, null);
    }

    /**
     * Toggle request status (ACTIVE <-> PAUSED).
     */
    public WantedItemResponse toggleStatus(Long id, WantedItemStatus newStatus, String studentId, String studentEmail) {
        WantedItem item = wantedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wanted item request not found with ID: " + id));

        if (!isOwner(item, studentId, studentEmail)) {
            throw new UnauthorizedException("You are not authorized to modify another student's wanted item request.");
        }

        item.setStatus(newStatus != null ? newStatus : (item.getStatus() == WantedItemStatus.ACTIVE ? WantedItemStatus.PAUSED : WantedItemStatus.ACTIVE));
        WantedItem updated = wantedItemRepository.save(item);

        if (updated.getStatus() == WantedItemStatus.ACTIVE) {
            scanExistingActiveListingsForMatches(updated);
        }

        List<WantedMatchDto> matches = wantedItemMatchRepository.findByWantedItemIdOrderByMatchedAtDesc(updated.getId())
                .stream().filter(Objects::nonNull).map(WantedMatchDto::fromEntity).filter(Objects::nonNull).collect(Collectors.toList());

        return WantedItemResponse.fromEntity(updated, matches);
    }

    public WantedItemResponse toggleStatus(Long id, WantedItemStatus newStatus, String studentId) {
        return toggleStatus(id, newStatus, studentId, null);
    }

    public WantedItemResponse toggleWantedItemStatus(Long id, WantedItemStatus newStatus, String studentId) {
        return toggleStatus(id, newStatus, studentId, null);
    }

    /**
     * Delete wanted item request and associated match records.
     */
    public void deleteWantedItem(Long id, String studentId, String studentEmail) {
        WantedItem item = wantedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wanted item request not found with ID: " + id));

        if (!isOwner(item, studentId, studentEmail)) {
            throw new UnauthorizedException("You are not authorized to delete another student's wanted item request.");
        }

        wantedItemMatchRepository.deleteByWantedItemId(id);
        wantedItemRepository.delete(item);
    }

    public void deleteWantedItem(Long id, String studentId) {
        deleteWantedItem(id, studentId, null);
    }

    /**
     * Check a newly created or published listing against all active wanted items.
     * Guaranteed not to throw or break the listing transaction.
     */
    public void checkAndAlertMatches(Listing listing) {
        if (listing == null || listing.getStatus() != ListingStatus.ACTIVE) {
            return;
        }

        try {
            List<WantedItem> activeRequests = wantedItemRepository.findByStatus(WantedItemStatus.ACTIVE);
            if (activeRequests.isEmpty()) {
                return;
            }

            for (WantedItem req : activeRequests) {
                // Do not alert a seller about their own listing
                if (req.getStudentId() != null && listing.getSellerId() != null
                        && req.getStudentId().equalsIgnoreCase(listing.getSellerId())) {
                    continue;
                }
                if (req.getStudentEmail() != null && listing.getSellerEmail() != null
                        && req.getStudentEmail().equalsIgnoreCase(listing.getSellerEmail())) {
                    continue;
                }

                if (isMatch(req, listing)) {
                    triggerMatchAlert(req, listing);
                }
            }
        } catch (Exception ex) {
            log.error("Error during wanted item matching check for listing [id={}]: {}", listing.getId(), ex.getMessage(), ex);
        }
    }

    /**
     * Scan existing active listings when a new wanted item request is created or reactivated.
     */
    private void scanExistingActiveListingsForMatches(WantedItem req) {
        try {
            List<Listing> activeListings = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.ACTIVE);
            for (Listing listing : activeListings) {
                if (req.getStudentId() != null && listing.getSellerId() != null
                        && req.getStudentId().equalsIgnoreCase(listing.getSellerId())) {
                    continue;
                }
                if (req.getStudentEmail() != null && listing.getSellerEmail() != null
                        && req.getStudentEmail().equalsIgnoreCase(listing.getSellerEmail())) {
                    continue;
                }
                if (isMatch(req, listing)) {
                    triggerMatchAlert(req, listing);
                }
            }
        } catch (Exception ex) {
            log.warn("Could not scan existing active listings for wanted item [id={}]: {}", req.getId(), ex.getMessage());
        }
    }

    /**
     * Core matching logic comparing wanted item criteria against listing details.
     */
    public boolean isMatch(WantedItem req, Listing listing) {
        // 1. Budget check: starting price must be <= max budget
        BigDecimal startingPrice = listing.getStartingPrice();
        if (startingPrice == null || startingPrice.compareTo(req.getMaxBudget()) > 0) {
            return false;
        }

        // If listing already has bids, verify current leading price is also within budget
        BigDecimal currentPrice = listing.getCurrentPrice() != null ? listing.getCurrentPrice() : startingPrice;
        if (currentPrice.compareTo(req.getMaxBudget()) > 0) {
            return false;
        }

        // 2. Category check (if specified and not "All")
        if (req.getCategory() != null && !req.getCategory().isBlank() && !req.getCategory().equalsIgnoreCase("All")) {
            if (listing.getCategory() == null || !listing.getCategory().trim().equalsIgnoreCase(req.getCategory().trim())) {
                return false;
            }
        }

        // 3. Condition compatibility check
        if (!isConditionCompatible(listing.getCondition(), req.getPreferredCondition())) {
            return false;
        }

        // 4. Normalized keyword matching (avoiding false substring matches)
        return matchesKeywords(req.getItemName(), listing.getTitle(), listing.getDescription());
    }

    /**
     * Checks keyword matching using whole word token boundaries.
     */
    private boolean matchesKeywords(String wantedKeywords, String listingTitle, String listingDescription) {
        if (wantedKeywords == null || wantedKeywords.isBlank()) {
            return true;
        }

        String targetText = ((listingTitle != null ? listingTitle : "") + " " +
                             (listingDescription != null ? listingDescription : "")).toLowerCase();

        // Extract tokens (letters and numbers, length >= 2)
        String[] tokens = wantedKeywords.toLowerCase().split("[^a-zA-Z0-9]+");

        // Filter out common stopwords
        Set<String> stopwords = Set.of("the", "and", "for", "with", "any", "some", "need", "looking", "want", "wanted");
        List<String> validKeywords = Arrays.stream(tokens)
                .filter(t -> t.length() >= 2 && !stopwords.contains(t))
                .collect(Collectors.toList());

        if (validKeywords.isEmpty()) {
            validKeywords = Arrays.stream(tokens).filter(t -> t.length() >= 2).collect(Collectors.toList());
        }

        if (validKeywords.isEmpty()) {
            return true;
        }

        // Require at least one significant keyword to match as a full word boundary
        for (String kw : validKeywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(targetText).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if listing's condition meets the student's preferred condition.
     */
    private boolean isConditionCompatible(ItemCondition listingCondition, String preferredCondition) {
        if (preferredCondition == null || preferredCondition.isBlank() || preferredCondition.equalsIgnoreCase("ANY")) {
            return true;
        }
        if (listingCondition == null) {
            return true;
        }

        String pref = preferredCondition.trim().toUpperCase().replace(" ", "_");

        // Hierarchy rank: higher or equal quality is acceptable
        int listingRank = getConditionRank(listingCondition.name());
        int preferredRank = getConditionRank(pref);

        return listingRank >= preferredRank;
    }

    private int getConditionRank(String conditionStr) {
        if (conditionStr == null) return 0;
        return switch (conditionStr) {
            case "BRAND_NEW", "NEW" -> 5;
            case "LIKE_NEW" -> 4;
            case "EXCELLENT" -> 3;
            case "GOOD" -> 2;
            case "FAIR" -> 1;
            default -> 0;
        };
    }

    /**
     * Save match and send deduplicated in-app notification & email alert.
     */
    private void triggerMatchAlert(WantedItem req, Listing listing) {
        // Prevent duplicate alerts for the same request and listing
        if (wantedItemMatchRepository.existsByWantedItemIdAndListingId(req.getId(), listing.getId())) {
            log.debug("Duplicate match alert skipped [wantedItemId={}, listingId={}]", req.getId(), listing.getId());
            return;
        }

        String coverPhoto = (listing.getPhotoUrls() != null && !listing.getPhotoUrls().isEmpty())
                ? listing.getPhotoUrls().get(0)
                : null;

        WantedItemMatch match = new WantedItemMatch(
                req.getId(),
                listing.getId(),
                listing.getTitle(),
                listing.getStartingPrice(),
                coverPhoto
        );
        wantedItemMatchRepository.save(match);

        // Format price in INR (₹)
        String priceText = listing.getStartingPrice() != null ? ("₹" + listing.getStartingPrice().toPlainString()) : "a great price";
        String alertMessage = "Your wanted item is available! A " + listing.getTitle() + " matching your request is listed for " + priceText + ". View auction.";

        // Send in-app notification
        notificationService.sendNotification(
                req.getStudentId(),
                req.getStudentName(),
                "🎯 Wanted Item Match Found!",
                alertMessage,
                NotificationType.WANTED_ITEM_MATCH,
                listing.getId(),
                listing.getTitle()
        );

        log.info("🎯 Wanted Item Match Alert triggered for student [{}] on lot [#{} - {}]",
                req.getStudentId(), listing.getId(), listing.getTitle());
    }
}
