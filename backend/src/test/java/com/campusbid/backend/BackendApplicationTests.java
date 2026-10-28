package com.campusbid.backend;

import com.campusbid.backend.dto.CreateListingRequest;
import com.campusbid.backend.dto.ListingResponse;
import com.campusbid.backend.dto.UpdateListingRequest;
import com.campusbid.backend.exception.UnauthorizedException;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private ListingService listingService;

	@Test
	@DisplayName("Context loads successfully")
	void contextLoads() {
		assertNotNull(listingService);
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
}
