package com.campusbid.backend;

import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.WantedItemRequest;
import com.campusbid.backend.dto.WantedItemResponse;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.*;
import com.campusbid.backend.repository.NotificationRepository;
import com.campusbid.backend.repository.WantedItemMatchRepository;
import com.campusbid.backend.repository.WantedItemRepository;
import com.campusbid.backend.service.ListingService;
import com.campusbid.backend.service.WantedItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WantedItemTests {

    @Autowired
    private WantedItemService wantedItemService;

    @Autowired
    private ListingService listingService;

    @Autowired
    private WantedItemRepository wantedItemRepository;

    @Autowired
    private WantedItemMatchRepository wantedItemMatchRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private CreateListingRequest buildListing(String title, String desc, String category,
                                              ItemCondition condition, BigDecimal startingPrice,
                                              String sellerId, String sellerName, String sellerEmail) {
        CreateListingRequest req = new CreateListingRequest();
        req.setTitle(title);
        req.setDescription(desc);
        req.setCategory(category);
        req.setCondition(condition);
        req.setStartingPrice(startingPrice);
        req.setMinBidIncrement(new BigDecimal("50.00"));
        req.setClosingTime(LocalDateTime.now().plusDays(3));
        req.setSellerId(sellerId);
        req.setSellerName(sellerName);
        req.setSellerEmail(sellerEmail);
        req.setPhotoUrls(List.of("https://images.unsplash.com/photo-campus.jpg"));
        return req;
    }

    @Test
    @DisplayName("1. Student A saves bicycle request (₹2000 budget), Student B lists for ₹1800 -> Student A receives exactly 1 notification")
    void testWantedItemMatchSuccess() {
        String studentAId = "stu-101";
        String studentBId = "stu-102";

        // Student A posts wanted item for "Mountain Bicycle" with max budget 2000
        WantedItemRequest wantedReq = new WantedItemRequest(
                "Mountain Bicycle",
                "Sports & Outdoors",
                new BigDecimal("2000.00"),
                "GOOD",
                "Looking for a sturdy commuter bike"
        );
        WantedItemResponse savedWanted = wantedItemService.createWantedItem(
                wantedReq, studentAId, "Alice Walker", "alice_test101@campus.edu"
        );
        assertNotNull(savedWanted);
        assertEquals(WantedItemStatus.ACTIVE, savedWanted.getStatus());

        // Student B publishes matching listing for 1800
        CreateListingRequest listingReq = buildListing(
                "Hero Sprint Mountain Bicycle",
                "Smooth gears, sturdy frame, campus commuter",
                "Sports & Outdoors",
                ItemCondition.LIKE_NEW,
                new BigDecimal("1800.00"),
                studentBId,
                "Bob Vance",
                "bob_test102@campus.edu"
        );
        ListingResponse listing = listingService.createListing(listingReq);
        assertNotNull(listing);

        // Verify Student A received exactly 1 in-app notification of type WANTED_ITEM_MATCH for Bob's listing
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId);
        long matchCount = notifs.stream()
                .filter(n -> n.getType() == NotificationType.WANTED_ITEM_MATCH && listing.getId().equals(n.getRelatedListingId()))
                .count();
        assertEquals(1, matchCount);

        Notification matchNotif = notifs.stream()
                .filter(n -> n.getType() == NotificationType.WANTED_ITEM_MATCH && listing.getId().equals(n.getRelatedListingId()))
                .findFirst()
                .orElseThrow();
        assertTrue(matchNotif.getMessage().contains("Mountain Bicycle") || matchNotif.getMessage().contains("Hero Sprint"));
        assertTrue(matchNotif.getMessage().contains("1,800") || matchNotif.getMessage().contains("1800"));
        assertEquals(listing.getId(), matchNotif.getRelatedListingId());

        // Verify deduplication match record exists
        assertTrue(wantedItemMatchRepository.existsByWantedItemIdAndListingId(savedWanted.getId(), listing.getId()));
    }

    @Test
    @DisplayName("2. Listing with starting price exceeding max budget does NOT trigger alert")
    void testOverBudgetNoAlert() {
        String studentAId = "stu-201";
        String studentBId = "stu-202";

        // Alice budget ₹2,000
        WantedItemRequest wantedReq = new WantedItemRequest(
                "Bicycle",
                "Sports & Outdoors",
                new BigDecimal("2000.00"),
                "ANY",
                "Need cycle"
        );
        wantedItemService.createWantedItem(wantedReq, studentAId, "Alice Budget", "alice_budget@campus.edu");

        // Bob lists for ₹2,500 (> ₹2,000)
        CreateListingRequest listingReq = buildListing(
                "Bicycle Road Champion",
                "Lightweight racing bike with accessories",
                "Sports & Outdoors",
                ItemCondition.LIKE_NEW,
                new BigDecimal("2500.00"),
                studentBId,
                "Bob Budget",
                "bob_budget@campus.edu"
        );
        ListingResponse bobLot = listingService.createListing(listingReq);

        // Alice should have NO notifications for Bob's over-budget lot
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId);
        assertFalse(notifs.stream().anyMatch(n -> bobLot.getId().equals(n.getRelatedListingId())));
    }

    @Test
    @DisplayName("3. Incompatible condition (wanted NEW, seller offers FAIR) does NOT trigger alert")
    void testConditionMismatchNoAlert() {
        String studentAId = "stu-301";
        String studentBId = "stu-302";

        // Alice wants BRAND_NEW textbook
        WantedItemRequest wantedReq = new WantedItemRequest(
                "Calculus Early Transcendentals",
                "Books & Notes",
                new BigDecimal("1500.00"),
                "BRAND_NEW",
                "Only sealed copies"
        );
        wantedItemService.createWantedItem(wantedReq, studentAId, "Alice Cond", "alice_cond@campus.edu");

        // Bob offers FAIR condition
        CreateListingRequest listingReq = buildListing(
                "Calculus Early Transcendentals 9th Edition",
                "Heavily highlighted and dog-eared campus textbook",
                "Books & Notes",
                ItemCondition.FAIR,
                new BigDecimal("800.00"),
                studentBId,
                "Bob Cond",
                "bob_cond@campus.edu"
        );
        listingService.createListing(listingReq);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId);
        assertEquals(0, notifs.size());
    }

    @Test
    @DisplayName("4. Paused wanted request does NOT trigger alerts on new listings")
    void testPausedRequestNoAlert() {
        String studentAId = "stu-401";
        String studentBId = "stu-402";

        WantedItemRequest wantedReq = new WantedItemRequest(
                "SuperRareCasioSynthCalculator",
                "Electronics",
                new BigDecimal("800.00"),
                "ANY",
                "SuperRareCasioSynthCalculator fx-991EX"
        );
        WantedItemResponse saved = wantedItemService.createWantedItem(
                wantedReq, studentAId, "Alice Paused", "alice_paused@campus.edu"
        );
        assertEquals(WantedItemStatus.ACTIVE, saved.getStatus());

        // Student pauses the request
        wantedItemService.toggleWantedItemStatus(saved.getId(), WantedItemStatus.PAUSED, studentAId);

        int countBefore = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId).size();

        // Bob lists matching calculator
        CreateListingRequest listingReq = buildListing(
                "SuperRareCasioSynthCalculator fx-991EX Special",
                "Working perfectly with slide-on case",
                "Electronics",
                ItemCondition.LIKE_NEW,
                new BigDecimal("600.00"),
                studentBId,
                "Bob Paused",
                "bob_paused@campus.edu"
        );
        ListingResponse bobLot = listingService.createListing(listingReq);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId);
        assertEquals(countBefore, notifs.size());
        assertFalse(notifs.stream().anyMatch(n -> bobLot.getId().equals(n.getRelatedListingId())));
    }

    @Test
    @DisplayName("5. Seller cannot trigger alerts for their own listing matching their own wanted request")
    void testSelfMatchIgnored() {
        String studentAId = "stu-501";

        WantedItemRequest wantedReq = new WantedItemRequest(
                "Desk Lamp",
                "Dorm Essentials",
                new BigDecimal("500.00"),
                "ANY",
                "LED warm light"
        );
        wantedItemService.createWantedItem(wantedReq, studentAId, "Alice Self", "alice_self@campus.edu");

        // Alice lists her own desk lamp
        CreateListingRequest listingReq = buildListing(
                "Desk Lamp with USB port",
                "Warm LED lamp with touch dimming",
                "Dorm Essentials",
                ItemCondition.LIKE_NEW,
                new BigDecimal("400.00"),
                studentAId,
                "Alice Self",
                "alice_self@campus.edu"
        );
        listingService.createListing(listingReq);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(studentAId);
        assertEquals(0, notifs.size());
    }

    @Test
    @DisplayName("6. Student cannot access, edit, or delete another student's wanted item")
    void testAuthorizationIsolation() {
        String studentAId = "stu-601";
        String studentBId = "stu-602";

        WantedItemRequest wantedReq = new WantedItemRequest(
                "Monitor",
                "Electronics",
                new BigDecimal("5000.00"),
                "GOOD",
                "24 inch IPS"
        );
        WantedItemResponse saved = wantedItemService.createWantedItem(
                wantedReq, studentAId, "Alice Auth", "alice_auth@campus.edu"
        );

        // Student B tries to retrieve Alice's wanted item -> Must throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () ->
                wantedItemService.getWantedItemById(saved.getId(), studentBId)
        );

        // Student B tries to update Alice's wanted item -> Must throw UnauthorizedException
        WantedItemRequest updateReq = new WantedItemRequest(
                "Hacked Monitor",
                "Electronics",
                new BigDecimal("100.00"),
                "GOOD",
                "Hacked"
        );
        assertThrows(UnauthorizedException.class, () ->
                wantedItemService.updateWantedItem(saved.getId(), updateReq, studentBId)
        );

        // Student B tries to delete Alice's wanted item -> Must throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () ->
                wantedItemService.deleteWantedItem(saved.getId(), studentBId)
        );
    }
}
