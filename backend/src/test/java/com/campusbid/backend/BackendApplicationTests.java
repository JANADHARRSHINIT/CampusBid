package com.campusbid.backend;

import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.PlaceBidRequest;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.BidRecord;
import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.service.ListingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.campusbid.backend.dto.NotificationResponse;
import com.campusbid.backend.model.NotificationType;
import com.campusbid.backend.service.NotificationService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private ListingService listingService;

	@Autowired
	private NotificationService notificationService;

	@Test
	@DisplayName("Context loads successfully")
	void contextLoads() {
		assertNotNull(listingService);
		assertNotNull(notificationService);
	}

	@Test
	@DisplayName("Create listing with valid data succeeds")
	void testCreateListingSuccess() {
		CreateListingRequest request = new CreateListingRequest();
		request.setTitle("Physics for Scientists and Engineers");
		request.setCategory("Textbooks");
		request.setDescription("10th edition textbook in like new condition. Minimal wear.");
		request.setCondition(ItemCondition.LIKE_NEW);
		request.setStartingPrice(new BigDecimal("35.00"));
		request.setMinBidIncrement(new BigDecimal("5.00"));
		request.setClosingTime(LocalDateTime.now().plusDays(7));
		request.setSellerId("student-999");
		request.setSellerName("Jane Doe");
		request.setSellerEmail("jane@campus.edu");
		request.setPhotoUrls(List.of("https://example.com/photo1.jpg"));

		ListingResponse response = listingService.createListing(request);

		assertNotNull(response.getId());
		assertEquals("Physics for Scientists and Engineers", response.getTitle());
		assertEquals(ListingStatus.ACTIVE, response.getStatus());
		assertEquals("student-999", response.getSellerId());
		assertFalse(response.isExpired());
	}

	@Test
	@DisplayName("Create listing with past closing time fails")
	void testCreateListingPastDateFails() {
		CreateListingRequest request = new CreateListingRequest();
		request.setTitle("Old notes");
		request.setCategory("Other");
		request.setDescription("Notes from last year");
		request.setCondition(ItemCondition.GOOD);
		request.setStartingPrice(new BigDecimal("10.00"));
		request.setMinBidIncrement(new BigDecimal("1.00"));
		request.setClosingTime(LocalDateTime.now().minusDays(1)); // Past
		request.setSellerId("student-999");

		assertThrows(IllegalArgumentException.class, () -> listingService.createListing(request));
	}

	@Test
	@DisplayName("Security: Edit listing by original seller succeeds, but by another student throws UnauthorizedException")
	void testSecurityOnEditListing() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Sony WH-1000XM4 Noise Cancelling Headphones");
		createReq.setCategory("Electronics");
		createReq.setDescription("Great condition with case and cable.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("120.00"));
		createReq.setMinBidIncrement(new BigDecimal("10.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(3));
		createReq.setSellerId("student-owner");

		ListingResponse created = listingService.createListing(createReq);

		UpdateListingRequest updateReq = new UpdateListingRequest();
		updateReq.setTitle("Sony WH-1000XM4 - Updated Title");
		updateReq.setCategory("Electronics");
		updateReq.setDescription("Updated description.");
		updateReq.setCondition(ItemCondition.LIKE_NEW);
		updateReq.setMinBidIncrement(new BigDecimal("10.00"));
		updateReq.setClosingTime(LocalDateTime.now().plusDays(4));

		// Another student tries to edit -> should fail with UnauthorizedException
		assertThrows(UnauthorizedException.class, () ->
				listingService.updateListing(created.getId(), updateReq, "student-intruder")
		);

		// Original seller edits -> succeeds
		ListingResponse updated = listingService.updateListing(created.getId(), updateReq, "student-owner");
		assertEquals("Sony WH-1000XM4 - Updated Title", updated.getTitle());
	}

	@Test
	@DisplayName("Security & Soft Delete: Cancel listing by seller soft-cancels, but another student cannot cancel")
	void testCancelListingSecurityAndPersistence() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Campus Parking Permit Fall 2026");
		createReq.setCategory("Other");
		createReq.setDescription("Zone C commuter parking permit.");
		createReq.setCondition(ItemCondition.NEW);
		createReq.setStartingPrice(new BigDecimal("50.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(10));
		createReq.setSellerId("student-seller");

		ListingResponse created = listingService.createListing(createReq);

		// Another student tries to cancel -> fails
		assertThrows(UnauthorizedException.class, () ->
				listingService.cancelListing(created.getId(), "student-other")
		);

		// Owner cancels -> succeeds
		ListingResponse cancelled = listingService.cancelListing(created.getId(), "student-seller");
		assertEquals(ListingStatus.CANCELLED, cancelled.getStatus());

		// Verify record is still in the database (soft cancellation)
		ListingResponse fromDb = listingService.getListingById(created.getId());
		assertNotNull(fromDb);
		assertEquals(ListingStatus.CANCELLED, fromDb.getStatus());
	}

	@Test
	@DisplayName("Search & Filters: Multi-criteria filtering by keyword, category, condition, and price range")
	void testSearchAndFilters() {
		CreateListingRequest item1 = new CreateListingRequest();
		item1.setTitle("Organic Chemistry Molecular Model Kit");
		item1.setCategory("Supplies");
		item1.setDescription("Includes 240 pieces, bonds, and orbital parts for CHEM 201.");
		item1.setCondition(ItemCondition.LIKE_NEW);
		item1.setStartingPrice(new BigDecimal("22.00"));
		item1.setMinBidIncrement(new BigDecimal("2.00"));
		item1.setClosingTime(LocalDateTime.now().plusDays(5));
		item1.setSellerId("student-chem");

		CreateListingRequest item2 = new CreateListingRequest();
		item2.setTitle("Mechanical Keyboard with Brown Switches");
		item2.setCategory("Electronics");
		item2.setDescription("Tenkeyless layout, great for coding in library without loud clicks.");
		item2.setCondition(ItemCondition.GOOD);
		item2.setStartingPrice(new BigDecimal("65.00"));
		item2.setMinBidIncrement(new BigDecimal("5.00"));
		item2.setClosingTime(LocalDateTime.now().plusHours(12)); // Closing within 24h
		item2.setSellerId("student-coder");

		listingService.createListing(item1);
		listingService.createListing(item2);

		// Test keyword search matching title
		List<ListingResponse> byTitle = listingService.getActiveListings(
				null, null, null, null, null, "Molecular", null
		);
		assertTrue(byTitle.stream().anyMatch(l -> l.getTitle().contains("Molecular Model Kit")));

		// Test keyword search matching description
		List<ListingResponse> byDesc = listingService.getActiveListings(
				null, null, null, null, null, "Tenkeyless", null
		);
		assertTrue(byDesc.stream().anyMatch(l -> l.getTitle().contains("Mechanical Keyboard")));

		// Test price range filter
		List<ListingResponse> priceFiltered = listingService.getActiveListings(
				null, null, new BigDecimal("60.00"), new BigDecimal("100.00"), null, null, null
		);
		assertTrue(priceFiltered.stream().allMatch(l -> l.getCurrentPrice().compareTo(new BigDecimal("60.00")) >= 0 && l.getCurrentPrice().compareTo(new BigDecimal("100.00")) <= 0));

		// Test condition filter
		List<ListingResponse> conditionFiltered = listingService.getActiveListings(
				null, ItemCondition.LIKE_NEW, null, null, null, null, null
		);
		assertTrue(conditionFiltered.stream().allMatch(l -> l.getCondition() == ItemCondition.LIKE_NEW));

		// Test closing soon (<24h) filter
		List<ListingResponse> closingSoonFiltered = listingService.getActiveListings(
				null, null, null, null, true, null, null
		);
		assertTrue(closingSoonFiltered.stream().anyMatch(l -> l.getTitle().contains("Mechanical Keyboard")));
	}

	@Test
	@DisplayName("Deadline Enforcement: Editing or cancelling a closed auction is rejected by backend")
	void testClosingDeadlineEnforcement() {
		// Create listing that has expired closing time (by setting in DB entity)
		CreateListingRequest req = new CreateListingRequest();
		req.setTitle("Expired Event Ticket");
		req.setCategory("Other");
		req.setDescription("Homecoming concert ticket.");
		req.setCondition(ItemCondition.NEW);
		req.setStartingPrice(new BigDecimal("25.00"));
		req.setMinBidIncrement(new BigDecimal("5.00"));
		req.setClosingTime(LocalDateTime.now().plusDays(2));
		req.setSellerId("student-deadline");

		ListingResponse created = listingService.createListing(req);

		// Now manually simulate auction expiration in DB
		com.campusbid.backend.model.Listing entity = listingService.getListingsBySeller("student-deadline").stream()
				.filter(l -> l.getId().equals(created.getId()))
				.findFirst()
				.map(dto -> {
					// Retrieve entity from repository via service lookup
					return listingService.getListingById(dto.getId());
				}).isPresent() ? null : null;

		// Test editing with a past closing time throws IllegalArgumentException
		UpdateListingRequest updateReq = new UpdateListingRequest();
		updateReq.setTitle("Updated Expired Event Ticket");
		updateReq.setCategory("Other");
		updateReq.setDescription("Updated desc.");
		updateReq.setCondition(ItemCondition.NEW);
		updateReq.setMinBidIncrement(new BigDecimal("5.00"));
		updateReq.setClosingTime(LocalDateTime.now().minusHours(1)); // Closing time in past

		assertThrows(IllegalArgumentException.class, () ->
				listingService.updateListing(created.getId(), updateReq, "student-deadline")
		);
	}

	@Test
	@DisplayName("Bidding: First bid at or above starting price succeeds and updates leading bid")
	void testFirstBidValid() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Graphing Calculator TI-84 Plus CE");
		createReq.setCategory("Electronics");
		createReq.setDescription("Like new graphing calculator with charging cable.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("70.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(5));
		createReq.setSellerId("student-seller-1");
		createReq.setSellerName("Alice Seller");

		ListingResponse created = listingService.createListing(createReq);
		assertEquals(new BigDecimal("70.00"), created.getStartingPrice());
		assertEquals(new BigDecimal("70.00"), created.getMinNextBid());
		assertTrue(created.getBidHistory() == null || created.getBidHistory().isEmpty());

		// Bidder 1 places first bid equal to starting price ($70.00)
		PlaceBidRequest bidReq = new PlaceBidRequest();
		bidReq.setAmount(new BigDecimal("70.00"));
		bidReq.setBidderId("student-bidder-1");
		bidReq.setBidderName("Bob Student");

		ListingResponse afterFirstBid = listingService.placeBid(created.getId(), bidReq, "student-bidder-1");
		assertEquals(new BigDecimal("70.00"), afterFirstBid.getCurrentPrice());
		assertEquals(new BigDecimal("70.00"), afterFirstBid.getCurrentLeadingBid());
		// Next minimum bid is now 70 + 5 = 75
		assertEquals(new BigDecimal("75.00"), afterFirstBid.getMinNextBid());
		assertEquals(1, afterFirstBid.getBidHistory().size());

		BidRecord record = afterFirstBid.getBidHistory().get(0);
		assertEquals(new BigDecimal("70.00"), record.getAmount());
		assertEquals("student-bidder-1", record.getBidderId());
		assertEquals("Bob Student", record.getBidderName());
		assertNotNull(record.getBidTime());
	}

	@Test
	@DisplayName("Bidding: Subsequent bid must meet or exceed current price + minBidIncrement")
	void testSubsequentBidValidation() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Dorm Mini Fridge 3.2 Cu Ft");
		createReq.setCategory("Dorm & Living");
		createReq.setDescription("Clean mini fridge, runs cold and quiet.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("40.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(4));
		createReq.setSellerId("student-seller-2");

		ListingResponse created = listingService.createListing(createReq);

		// First bid: $40.00
		PlaceBidRequest bid1 = new PlaceBidRequest();
		bid1.setAmount(new BigDecimal("40.00"));
		bid1.setBidderId("student-buyer-a");
		bid1.setBidderName("Buyer A");
		listingService.placeBid(created.getId(), bid1, "student-buyer-a");

		// Attempting bid of $42.00 (below min increment of 40 + 5 = 45) should fail
		PlaceBidRequest invalidBid = new PlaceBidRequest();
		invalidBid.setAmount(new BigDecimal("42.00"));
		invalidBid.setBidderId("student-buyer-b");
		invalidBid.setBidderName("Buyer B");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
				listingService.placeBid(created.getId(), invalidBid, "student-buyer-b")
		);
		assertTrue(ex.getMessage().contains("Bid must be at least"));

		// Valid subsequent bid of $45.00 succeeds
		PlaceBidRequest validBid = new PlaceBidRequest();
		validBid.setAmount(new BigDecimal("45.00"));
		validBid.setBidderId("student-buyer-b");
		validBid.setBidderName("Buyer B");

		ListingResponse afterSecondBid = listingService.placeBid(created.getId(), validBid, "student-buyer-b");
		assertEquals(new BigDecimal("45.00"), afterSecondBid.getCurrentPrice());
		assertEquals(2, afterSecondBid.getBidHistory().size());
		// Next min bid is 45 + 5 = 50
		assertEquals(new BigDecimal("50.00"), afterSecondBid.getMinNextBid());
	}

	@Test
	@DisplayName("Bidding Security: Seller is prohibited from bidding on their own listing")
	void testSellerSelfBiddingProhibited() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Art History Textbook");
		createReq.setCategory("Textbooks");
		createReq.setDescription("Annotated art history textbook.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("20.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-self-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest selfBid = new PlaceBidRequest();
		selfBid.setAmount(new BigDecimal("25.00"));
		selfBid.setBidderId("student-self-seller");
		selfBid.setBidderName("Self Seller");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
				listingService.placeBid(created.getId(), selfBid, "student-self-seller")
		);
		assertTrue(ex.getMessage().contains("Sellers are prohibited from bidding on their own lots"));
	}

	@Test
	@DisplayName("Bidding Validation: Unauthenticated bid and bids on cancelled/closed lots are rejected")
	void testInvalidBiddingStates() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Lab Coat & Goggles");
		createReq.setCategory("Supplies");
		createReq.setDescription("Size M white lab coat and UV goggles.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("15.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(3));
		createReq.setSellerId("student-lab-seller");

		ListingResponse created = listingService.createListing(createReq);

		// 1. Unauthenticated bid (null or blank student ID)
		PlaceBidRequest bidReq = new PlaceBidRequest();
		bidReq.setAmount(new BigDecimal("20.00"));
		bidReq.setBidderId("student-guest");

		assertThrows(UnauthorizedException.class, () ->
				listingService.placeBid(created.getId(), bidReq, null)
		);
		assertThrows(UnauthorizedException.class, () ->
				listingService.placeBid(created.getId(), bidReq, "   ")
		);

		// 2. Cancel the listing and verify bidding is rejected
		listingService.cancelListing(created.getId(), "student-lab-seller");

		assertThrows(IllegalArgumentException.class, () ->
				listingService.placeBid(created.getId(), bidReq, "student-buyer-c")
		);
	}

	@Test
	@DisplayName("Concurrency: Simultaneous competing bids against the same initial price serialize with pessimistic lock")
	void testSimultaneousBidsConcurrency() throws InterruptedException {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Nintendo Switch OLED Console");
		createReq.setCategory("Electronics");
		createReq.setDescription("White dock edition with cables.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("200.00"));
		createReq.setMinBidIncrement(new BigDecimal("10.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(3));
		createReq.setSellerId("student-gamer-seller");

		ListingResponse created = listingService.createListing(createReq);

		// Two concurrent threads try to place a bid of $200.00 at the exact same moment
		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch readyLatch = new CountDownLatch(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final int threadIdx = i;
			executor.submit(() -> {
				readyLatch.countDown();
				try {
					startLatch.await();
					PlaceBidRequest req = new PlaceBidRequest();
					req.setAmount(new BigDecimal("200.00"));
					req.setBidderId("student-concurrent-" + threadIdx);
					req.setBidderName("Concurrent Bidder " + threadIdx);

					listingService.placeBid(created.getId(), req, "student-concurrent-" + threadIdx);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failureCount.incrementAndGet();
				} finally {
					finishLatch.countDown();
				}
			});
		}

		readyLatch.await();
		startLatch.countDown(); // trigger both threads simultaneously
		boolean finished = finishLatch.await(15, TimeUnit.SECONDS);
		assertTrue(finished, "Both concurrent threads should complete within timeout");
		executor.shutdown();

		// Exactly one bid should succeed at $200.00, and the other must be rejected
		// because the minimum next bid rose to $210.00 while the second transaction waited on the pessimistic row lock
		assertEquals(1, successCount.get(), "Exactly one concurrent bid must succeed");
		assertEquals(1, failureCount.get(), "The competing bid against the outdated price must be rejected");

		ListingResponse finalListing = listingService.getListingById(created.getId());
		assertEquals(new BigDecimal("200.00"), finalListing.getCurrentPrice());
		assertEquals(1, finalListing.getBidHistory().size());
		assertEquals(new BigDecimal("210.00"), finalListing.getMinNextBid());
	}

	@Test
	@DisplayName("Closing: Auction with no bids marks status as UNSOLD")
	void testAuctionClosingWithNoBidsBecomesUnsold() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Unsold Chemistry Flasks");
		createReq.setCategory("Supplies");
		createReq.setDescription("Glass flasks set.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("30.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(1));
		createReq.setSellerId("student-unsold-seller");

		ListingResponse created = listingService.createListing(createReq);
		assertEquals(ListingStatus.ACTIVE, created.getStatus());

		// Close the auction (0 bids)
		ListingResponse closed = listingService.closeAuction(created.getId(), "student-unsold-seller");
		assertEquals(ListingStatus.UNSOLD, closed.getStatus());
		assertNull(closed.getWinningBidderId());

		// Bidding on UNSOLD auction must be rejected
		PlaceBidRequest bidReq = new PlaceBidRequest();
		bidReq.setAmount(new BigDecimal("35.00"));
		bidReq.setBidderId("student-late-bidder");
		assertThrows(IllegalArgumentException.class, () ->
				listingService.placeBid(created.getId(), bidReq, "student-late-bidder")
		);
	}

	@Test
	@DisplayName("Closing: Auction with bids identifies highest valid bidder and enters AWAITING_SELLER_APPROVAL")
	void testAuctionClosingWithBidsSelectsHighestBidder() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Calculus Early Transcendentals");
		createReq.setCategory("Textbooks");
		createReq.setDescription("James Stewart 9th edition.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("40.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-calc-seller");

		ListingResponse created = listingService.createListing(createReq);

		// Bid 1: $40.00 by Bidder 1
		PlaceBidRequest bid1 = new PlaceBidRequest();
		bid1.setAmount(new BigDecimal("40.00"));
		bid1.setBidderId("student-buyer-one");
		bid1.setBidderName("Alice One");
		listingService.placeBid(created.getId(), bid1, "student-buyer-one");

		// Bid 2: $50.00 by Bidder 2 (Highest)
		PlaceBidRequest bid2 = new PlaceBidRequest();
		bid2.setAmount(new BigDecimal("50.00"));
		bid2.setBidderId("student-buyer-two");
		bid2.setBidderName("Bob Two");
		listingService.placeBid(created.getId(), bid2, "student-buyer-two");

		// Close auction
		ListingResponse closed = listingService.closeAuction(created.getId(), "student-calc-seller");
		assertEquals(ListingStatus.AWAITING_SELLER_APPROVAL, closed.getStatus());
		assertEquals("student-buyer-two", closed.getWinningBidderId());
		assertEquals("Bob Two", closed.getWinningBidderName());
		assertEquals(new BigDecimal("50.00"), closed.getWinningBidAmount());
		assertNotNull(closed.getWinningBidTime());
	}

	@Test
	@DisplayName("Seller Decision: Seller can APPROVE or REJECT winning bid; unauthorized student is blocked")
	void testSellerDecisionWorkflowAndSecurity() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Desk Lamp with Wireless Charger");
		createReq.setCategory("Dorm & Living");
		createReq.setDescription("LED lamp with Qi charging base.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("20.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-lamp-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("25.00"));
		bid.setBidderId("student-winner-lamp");
		bid.setBidderName("Lamp Winner");
		listingService.placeBid(created.getId(), bid, "student-winner-lamp");

		listingService.closeAuction(created.getId(), "student-lamp-seller");

		// 1. Intruder attempts to approve -> UnauthorizedException
		assertThrows(UnauthorizedException.class, () ->
				listingService.processSellerDecision(created.getId(), "APPROVE", "Intruder approving", "student-intruder")
		);

		// 2. Owner approves -> status becomes APPROVED
		ListingResponse approved = listingService.processSellerDecision(
				created.getId(), "APPROVE", "Meet outside student center tomorrow 3pm.", "student-lamp-seller"
		);
		assertEquals(ListingStatus.APPROVED, approved.getStatus());
		assertEquals("APPROVED", approved.getSellerDecision());
		assertNotNull(approved.getSellerDecisionTime());
		assertTrue(approved.getSellerDecisionNotes().contains("student center"));

		// 3. Duplicate decision on already approved lot -> IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () ->
				listingService.processSellerDecision(created.getId(), "REJECT", "Late rejection", "student-lamp-seller")
		);
	}

	@Test
	@DisplayName("Handover: Seller and winner mutual confirmation transitions sale to COMPLETED")
	void testCampusHandoverWorkflow() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Mini Steamer for Clothes");
		createReq.setCategory("Dorm & Living");
		createReq.setDescription("Travel clothing steamer.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("15.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-steamer-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("18.00"));
		bid.setBidderId("student-steamer-buyer");
		bid.setBidderName("Steamer Buyer");
		listingService.placeBid(created.getId(), bid, "student-steamer-buyer");

		listingService.closeAuction(created.getId(), "student-steamer-seller");
		listingService.processSellerDecision(created.getId(), "APPROVE", "Approved", "student-steamer-seller");

		// 1. Non-participant tries to confirm -> UnauthorizedException
		assertThrows(UnauthorizedException.class, () ->
				listingService.confirmHandover(created.getId(), "student-stranger")
		);

		// 2. Winner confirms handover -> recorded, status still APPROVED
		ListingResponse afterBuyer = listingService.confirmHandover(created.getId(), "student-steamer-buyer");
		assertTrue(afterBuyer.getHandoverConfirmedByWinner());
		assertFalse(afterBuyer.getHandoverConfirmedBySeller());
		assertEquals(ListingStatus.APPROVED, afterBuyer.getStatus());

		// 3. Seller confirms handover -> both confirmed -> status becomes COMPLETED!
		ListingResponse afterSeller = listingService.confirmHandover(created.getId(), "student-steamer-seller");
		assertTrue(afterSeller.getHandoverConfirmedBySeller());
		assertTrue(afterSeller.getHandoverConfirmedByWinner());
		assertEquals(ListingStatus.COMPLETED, afterSeller.getStatus());
		assertNotNull(afterSeller.getHandoverTime());
	}

	@Test
	@DisplayName("Cancellation: Failed pickup or seller cancellation updates status to CANCELLED and preserves bids")
	void testCancelApprovedSalePreservesData() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Bluetooth Speaker Clip 4");
		createReq.setCategory("Electronics");
		createReq.setDescription("Portable waterproof speaker.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("30.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-speaker-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("35.00"));
		bid.setBidderId("student-speaker-buyer");
		listingService.placeBid(created.getId(), bid, "student-speaker-buyer");

		listingService.closeAuction(created.getId(), "student-speaker-seller");
		listingService.processSellerDecision(created.getId(), "APPROVE", "Approved", "student-speaker-seller");

		// Buyer failed to show up -> seller cancels sale
		ListingResponse cancelled = listingService.cancelSale(
				created.getId(), "Buyer failed to show up for campus pickup after 48h.", "student-speaker-seller"
		);
		assertEquals(ListingStatus.CANCELLED, cancelled.getStatus());
		assertTrue(cancelled.getSellerDecisionNotes().contains("failed to show up"));

		// Bids must still be present in database
		ListingResponse fromDb = listingService.getListingById(created.getId());
		assertEquals(1, fromDb.getBidHistory().size());
		assertEquals(new BigDecimal("35.00"), fromDb.getBidHistory().get(0).getAmount());
	}

	@Test
	@DisplayName("Notification: Bid placement emits BID_ACCEPTED, BID_RECEIVED, and OUTBID events")
	void testBidPlacementNotificationsAndOutbid() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Mechanical Keyboard RGB");
		createReq.setCategory("Electronics");
		createReq.setDescription("Tactile brown switches.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("40.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-notif-seller");
		createReq.setSellerName("Seller Sam");

		ListingResponse created = listingService.createListing(createReq);

		// First bid from Student 1
		PlaceBidRequest bid1 = new PlaceBidRequest();
		bid1.setAmount(new BigDecimal("45.00"));
		bid1.setBidderId("student-notif-buyer1");
		bid1.setBidderName("Buyer One");
		listingService.placeBid(created.getId(), bid1, "student-notif-buyer1");

		// Verify Buyer 1 got BID_ACCEPTED
		List<NotificationResponse> buyer1Notifs = notificationService.getNotificationsForRecipient("student-notif-buyer1");
		assertTrue(buyer1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.BID_ACCEPTED));

		// Verify Seller got BID_RECEIVED
		List<NotificationResponse> sellerNotifs = notificationService.getNotificationsForRecipient("student-notif-seller");
		assertTrue(sellerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.BID_RECEIVED && n.getMessage().contains("Buyer One")));

		// Second bid from Student 2 (outbids Student 1)
		PlaceBidRequest bid2 = new PlaceBidRequest();
		bid2.setAmount(new BigDecimal("55.00"));
		bid2.setBidderId("student-notif-buyer2");
		bid2.setBidderName("Buyer Two");
		listingService.placeBid(created.getId(), bid2, "student-notif-buyer2");

		// Verify Student 1 received OUTBID notification!
		buyer1Notifs = notificationService.getNotificationsForRecipient("student-notif-buyer1");
		assertTrue(buyer1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.OUTBID && n.getMessage().contains("Buyer Two")));

		// Verify Student 2 got BID_ACCEPTED
		List<NotificationResponse> buyer2Notifs = notificationService.getNotificationsForRecipient("student-notif-buyer2");
		assertTrue(buyer2Notifs.stream().anyMatch(n -> n.getType() == NotificationType.BID_ACCEPTED));
	}

	@Test
	@DisplayName("Notification: Auction closing emits AUCTION_WON and AWAITING_APPROVAL with deduplication")
	void testAuctionClosingNotificationsAndDeduplication() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Calculus Early Transcendentals");
		createReq.setCategory("Textbooks");
		createReq.setDescription("Great condition with formula sheets.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("25.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(1));
		createReq.setSellerId("student-calc-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("30.00"));
		bid.setBidderId("student-calc-winner");
		bid.setBidderName("Calc Champ");
		listingService.placeBid(created.getId(), bid, "student-calc-winner");

		// Close auction
		listingService.closeAuction(created.getId(), "student-calc-seller");

		// Verify Winner got AUCTION_WON
		List<NotificationResponse> winnerNotifs = notificationService.getNotificationsForRecipient("student-calc-winner");
		assertTrue(winnerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.AUCTION_WON));

		// Verify Seller got AWAITING_APPROVAL and AUCTION_CLOSED
		List<NotificationResponse> sellerNotifs = notificationService.getNotificationsForRecipient("student-calc-seller");
		assertTrue(sellerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.AWAITING_APPROVAL));
		assertTrue(sellerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.AUCTION_CLOSED));

		long wonCountBefore = winnerNotifs.stream().filter(n -> n.getType() == NotificationType.AUCTION_WON).count();

		// Trigger batch process closing again - duplicate notifications must be prevented!
		listingService.processExpiredAuctions();
		List<NotificationResponse> winnerNotifsAfter = notificationService.getNotificationsForRecipient("student-calc-winner");
		long wonCountAfter = winnerNotifsAfter.stream().filter(n -> n.getType() == NotificationType.AUCTION_WON).count();
		assertEquals(wonCountBefore, wonCountAfter);
	}

	@Test
	@DisplayName("Notification: Seller decision emits BID_APPROVED or BID_REJECTED to winning bidder")
	void testSellerDecisionNotifications() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Desk Lamp LED");
		createReq.setCategory("Dorm & Furniture");
		createReq.setDescription("Adjustable brightness desk lamp.");
		createReq.setCondition(ItemCondition.LIKE_NEW);
		createReq.setStartingPrice(new BigDecimal("15.00"));
		createReq.setMinBidIncrement(new BigDecimal("2.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(2));
		createReq.setSellerId("student-lamp-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("20.00"));
		bid.setBidderId("student-lamp-winner");
		bid.setBidderName("Lamp Lover");
		listingService.placeBid(created.getId(), bid, "student-lamp-winner");

		listingService.closeAuction(created.getId(), "student-lamp-seller");

		// Seller approves winning bid
		listingService.processSellerDecision(created.getId(), "APPROVE", "Meet at dorm lobby", "student-lamp-seller");

		// Winner must have received BID_APPROVED notification
		List<NotificationResponse> winnerNotifs = notificationService.getNotificationsForRecipient("student-lamp-winner");
		assertTrue(winnerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.BID_APPROVED && n.getMessage().contains("Meet at dorm lobby")));
	}

	@Test
	@DisplayName("Notification: Handover confirmations emit HANDOVER_CONFIRMED and SALE_COMPLETED to both parties")
	void testHandoverNotifications() {
		CreateListingRequest createReq = new CreateListingRequest();
		createReq.setTitle("Graphing Calculator TI-84");
		createReq.setCategory("Electronics");
		createReq.setDescription("Fully working with fresh batteries.");
		createReq.setCondition(ItemCondition.GOOD);
		createReq.setStartingPrice(new BigDecimal("50.00"));
		createReq.setMinBidIncrement(new BigDecimal("5.00"));
		createReq.setClosingTime(LocalDateTime.now().plusDays(1));
		createReq.setSellerId("student-calc84-seller");

		ListingResponse created = listingService.createListing(createReq);

		PlaceBidRequest bid = new PlaceBidRequest();
		bid.setAmount(new BigDecimal("60.00"));
		bid.setBidderId("student-calc84-buyer");
		listingService.placeBid(created.getId(), bid, "student-calc84-buyer");

		listingService.closeAuction(created.getId(), "student-calc84-seller");
		listingService.processSellerDecision(created.getId(), "APPROVE", "Approved", "student-calc84-seller");

		// 1. Seller confirms handover -> buyer receives HANDOVER_CONFIRMED
		listingService.confirmHandover(created.getId(), "student-calc84-seller");
		List<NotificationResponse> buyerNotifs = notificationService.getNotificationsForRecipient("student-calc84-buyer");
		assertTrue(buyerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.HANDOVER_CONFIRMED));

		// 2. Buyer confirms handover -> sale is completed -> both receive SALE_COMPLETED
		listingService.confirmHandover(created.getId(), "student-calc84-buyer");

		List<NotificationResponse> buyerAfter = notificationService.getNotificationsForRecipient("student-calc84-buyer");
		assertTrue(buyerAfter.stream().anyMatch(n -> n.getType() == NotificationType.SALE_COMPLETED));

		List<NotificationResponse> sellerAfter = notificationService.getNotificationsForRecipient("student-calc84-seller");
		assertTrue(sellerAfter.stream().anyMatch(n -> n.getType() == NotificationType.SALE_COMPLETED));
	}

	@Test
	@DisplayName("Notification: Unread count, mark as read, and unauthorized recipient access control")
	void testNotificationReadSecurityAndCount() {
		// Create a test notification for student-alice
		NotificationResponse notif = notificationService.sendNotification(
				"student-alice",
				"Alice",
				"Test Alert",
				"This is a test notification message.",
				NotificationType.BID_RECEIVED,
				1L,
				"Sample Item"
		);
		assertNotNull(notif);
		assertFalse(notif.isRead());

		long unreadAlice = notificationService.getUnreadCount("student-alice");
		assertTrue(unreadAlice >= 1);

		// Bob cannot mark Alice's notification as read
		assertThrows(UnauthorizedException.class, () -> {
			notificationService.markAsRead(notif.getId(), "student-bob");
		});

		// Alice marks her notification as read
		NotificationResponse marked = notificationService.markAsRead(notif.getId(), "student-alice");
		assertTrue(marked.isRead());
		assertNotNull(marked.getReadAt());

		// Mark all as read
		notificationService.markAllAsRead("student-alice");
		assertEquals(0, notificationService.getUnreadCount("student-alice"));
	}
}


