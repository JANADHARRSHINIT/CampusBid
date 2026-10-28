package com.campusbid.backend.service;

import com.campusbid.backend.dto.*;
import com.campusbid.backend.exception.ResourceNotFoundException;
import com.campusbid.backend.exception.UnauthorizedException;
import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.ListingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CopilotService {

    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);

    private final ListingService listingService;

    @Value("${campusbid.copilot.provider:fallback}")
    private String provider;

    @Value("${campusbid.copilot.api-key:}")
    private String apiKey;

    public CopilotService(ListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * Process natural-language chat request from student.
     * Maps requests to live backend auction data and returns structured suggestions.
     */
    public CopilotChatResponse processChat(CopilotChatRequest request, String studentId) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        String rawMessage = request.getMessage().trim();
        if (rawMessage.length() > 1000) {
            rawMessage = rawMessage.substring(0, 1000);
        }

        // Sanitize input: prevent script tags
        String sanitizedMessage = rawMessage.replaceAll("(?i)<script.*?>.*?</script.*?>", "")
                .replaceAll("<[^>]+>", "");

        String lower = sanitizedMessage.toLowerCase();

        // 1. Intent: Explain how bidding works / Bidding rules
        if (isExplainRulesIntent(lower)) {
            return handleExplainRules();
        }

        // 2. Intent: Ask for current highest bid / Item status
        if (isItemStatusIntent(lower) || (request.getContextListingId() != null && isAskBidOnContext(lower))) {
            return handleItemStatus(sanitizedMessage, lower, request.getContextListingId());
        }

        // 3. Intent: Bid assistance / Suggest a bid amount
        if (isBidAssistanceIntent(lower) || (request.getContextListingId() != null && isBidAssistanceForContext(lower))) {
            return handleBidAssistance(sanitizedMessage, lower, request.getContextListingId(), request.getMaxBudget(), studentId);
        }

        // 4. Intent: Search / Recommendations (e.g. "Find me a cycle under ₹2,000", "electronics closing soon", etc.)
        return handleSearchAndRecommendations(sanitizedMessage, lower);
    }

    /**
     * Confirms and executes a bid proposal after explicit student confirmation.
     * Enforces authentication, maximum budget protection, and delegates to core bidding business logic.
     */
    public ListingResponse confirmBid(CopilotConfirmBidRequest request, String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new UnauthorizedException("Authentication required. Please specify student identity (X-Student-Id).");
        }

        if (request.getListingId() == null) {
            throw new IllegalArgumentException("Listing ID is required to place a bid.");
        }

        if (request.getBidAmount() == null || request.getBidAmount().signum() <= 0) {
            throw new IllegalArgumentException("Bid amount must be a positive number.");
        }

        // Independent security validation: never bid above student's agreed maximum budget
        if (request.getMaxBudget() != null && request.getBidAmount().compareTo(request.getMaxBudget()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Safety Check Failed: Proposed bid of $%.2f exceeds your agreed maximum budget of $%.2f. Bid was not placed.",
                    request.getBidAmount(), request.getMaxBudget()
            ));
        }

        // Delegate to existing core bidding service (which validates status, closing deadline, seller ownership, min increment, locks)
        PlaceBidRequest bidRequest = new PlaceBidRequest();
        bidRequest.setAmount(request.getBidAmount());
        bidRequest.setBidderId(studentId.trim());

        return listingService.placeBid(request.getListingId(), bidRequest, studentId.trim());
    }

    // =========================================================================
    // Intent Handlers
    // =========================================================================

    private CopilotChatResponse handleExplainRules() {
        CopilotChatResponse response = new CopilotChatResponse();
        response.setIntent("EXPLAIN_RULES");
        response.setReply("""
### 🔨 How CampusBid Auction Bidding Works

CampusBid is a fair, campus-only auction marketplace for university students. Here are the core rules:

1. **Opening & Leading Bids:**
   - Every lot begins at a configured **Starting Price**.
   - If no bids have been placed yet, the first bid must be at least the starting price.
   - Subsequent bids must be at least the **Current Leading Bid + Minimum Bid Increment**.

2. **Fair Bidding & Ownership Rules:**
   - Sellers are **strictly prohibited** from bidding on their own lots.
   - All bids receive an official server-generated timestamp. No bid history is ever erased.

3. **Auction Closing & Winner Determination:**
   - Each lot has a live countdown and closing deadline.
   - When the deadline passes, new bids are strictly rejected.
   - The highest valid bidder is selected as the winning bidder.
   - Lots with zero bids are marked as **UNSOLD**.

4. **Seller Approval & Campus Handover:**
   - Sellers can **APPROVE** or **REJECT** the winning bid.
   - On approval, the item is reserved. Both seller and winning buyer must confirm mutual in-person campus handover to complete the sale.

5. **AI Copilot Safety (Mandatory Confirmation):**
   - I will **never place a bid automatically**. You must always review and click **Confirm Bid** before any bid is sent.
""");

        response.setSuggestedActions(List.of(
                "Find me a cycle under ₹2,000",
                "Show electronics auctions closing soon",
                "Which books are in good condition under ₹500?",
                "Show all active auctions"
        ));
        return response;
    }

    private CopilotChatResponse handleItemStatus(String rawMessage, String lower, Long contextListingId) {
        Long listingId = extractLotId(rawMessage, contextListingId);

        if (listingId == null) {
            // Try to find an active listing matching keywords in the message
            List<ListingResponse> candidates = listingService.getActiveListings(null, null, null, null, null, extractSearchKeyword(lower), null);
            if (!candidates.isEmpty()) {
                listingId = candidates.get(0).getId();
            }
        }

        if (listingId == null) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("ITEM_STATUS");
            response.setReply("Please specify which lot you would like to check (for example: *\"What is the highest bid for Lot #4?\"*) or open an auction details modal.");
            response.setSuggestedActions(List.of(
                    "Check Lot #1",
                    "Check Lot #4",
                    "Show all active auctions"
            ));
            return response;
        }

        ListingResponse listing;
        try {
            listing = listingService.getListingById(listingId);
        } catch (ResourceNotFoundException e) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("ITEM_STATUS");
            response.setReply(String.format("I couldn't find an auction matching **LOT #%d**. It may not exist or has been removed.", listingId));
            return response;
        }

        BigDecimal currentBid = listing.getCurrentLeadingBid() != null
                ? listing.getCurrentLeadingBid()
                : (listing.getCurrentPrice() != null ? listing.getCurrentPrice() : listing.getStartingPrice());

        int totalBids = listing.getTotalBids();
        BigDecimal minNext = listing.getMinNextBid() != null
                ? listing.getMinNextBid()
                : (totalBids > 0 ? currentBid.add(listing.getMinBidIncrement()) : listing.getStartingPrice());

        boolean isClosed = listing.getStatus() != ListingStatus.ACTIVE || listing.isExpired();

        String statusStr = isClosed
                ? (listing.getStatus() != null ? listing.getStatus().name() : "CLOSED")
                : "ACTIVE (Open for Bidding)";

        String reply = String.format("""
### 🏷️ Status for LOT #%03d: %s

- **Auction Status:** `%s`
- **Current %s:** **$%.2f** (₹%.2f equivalent)
- **Total Bids Placed:** %d
- **Starting Price:** $%.2f
- **Minimum Bid Increment:** +$%.2f
- **Minimum Valid Next Bid:** **$%.2f**
- **Seller:** %s (%s)
- **Closing Time:** %s
%s
""",
                listing.getId(),
                listing.getTitle(),
                statusStr,
                totalBids > 0 ? "Highest Leading Bid" : "Starting Price",
                currentBid,
                currentBid,
                totalBids,
                listing.getStartingPrice(),
                listing.getMinBidIncrement(),
                minNext,
                listing.getSellerName() != null ? listing.getSellerName() : listing.getSellerId(),
                listing.getSellerId(),
                listing.getClosingTime(),
                isClosed ? "\n> ⚠️ *This auction has concluded. Direct bids are no longer permitted.*" : "\n*Would you like assistance deciding a bid amount for this item?*"
        );

        CopilotChatResponse response = new CopilotChatResponse(reply, "ITEM_STATUS");
        response.setMatchingListings(List.of(listing));

        if (!isClosed) {
            response.setSuggestedActions(List.of(
                    String.format("Help me bid on Lot #%d", listing.getId()),
                    String.format("Suggest bid under $%.2f", minNext.add(listing.getMinBidIncrement().multiply(new BigDecimal(3)))),
                    "Explain how bidding works"
            ));
        }

        return response;
    }

    private CopilotChatResponse handleBidAssistance(String rawMessage, String lower, Long contextListingId, BigDecimal maxBudgetFromReq, String studentId) {
        Long listingId = extractLotId(rawMessage, contextListingId);

        if (listingId == null) {
            List<ListingResponse> candidates = listingService.getActiveListings(null, null, null, null, null, extractSearchKeyword(lower), null);
            if (!candidates.isEmpty()) {
                listingId = candidates.get(0).getId();
            }
        }

        if (listingId == null) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("BID_ASSISTANCE");
            response.setReply("Which lot would you like bid assistance for? You can tell me *\"Help me bid on Lot #4\"* or open an item details modal.");
            response.setSuggestedActions(List.of("Help me bid on Lot #1", "Help me bid on Lot #4"));
            return response;
        }

        ListingResponse listing;
        try {
            listing = listingService.getListingById(listingId);
        } catch (ResourceNotFoundException e) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("BID_ASSISTANCE");
            response.setReply(String.format("LOT #%d was not found.", listingId));
            return response;
        }

        // Check if seller owns listing
        if (studentId != null && listing.getSellerId() != null && listing.getSellerId().trim().equalsIgnoreCase(studentId.trim())) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("BID_ASSISTANCE");
            response.setReply(String.format("🔒 **Seller Bidding Prohibited**: You are the seller of LOT #%d (`%s`). In accordance with CampusBid fair auction rules, sellers cannot place bids on their own lots.", listing.getId(), listing.getTitle()));
            response.setMatchingListings(List.of(listing));
            return response;
        }

        // Check if closed
        if (listing.getStatus() != ListingStatus.ACTIVE || listing.isExpired()) {
            CopilotChatResponse response = new CopilotChatResponse();
            response.setIntent("BID_ASSISTANCE");
            response.setReply(String.format("⌛ **Auction Closed**: LOT #%d has already closed (Status: `%s`). Bidding is permanently closed.", listing.getId(), listing.getStatus()));
            response.setMatchingListings(List.of(listing));
            return response;
        }

        BigDecimal currentBid = listing.getCurrentLeadingBid() != null
                ? listing.getCurrentLeadingBid()
                : (listing.getCurrentPrice() != null ? listing.getCurrentPrice() : listing.getStartingPrice());
        int totalBids = listing.getTotalBids();
        BigDecimal increment = listing.getMinBidIncrement() != null ? listing.getMinBidIncrement() : new BigDecimal("1.00");

        BigDecimal minNextBid = listing.getMinNextBid() != null
                ? listing.getMinNextBid()
                : (totalBids > 0 ? currentBid.add(increment) : listing.getStartingPrice());

        // Extract student max budget if stated in chat message or passed in request
        BigDecimal budget = maxBudgetFromReq;
        if (budget == null) {
            budget = extractPrice(rawMessage);
        }

        BigDecimal proposedBid = minNextBid;
        boolean canAfford = true;
        String warning = null;

        if (budget != null) {
            if (proposedBid.compareTo(budget) > 0) {
                canAfford = false;
                warning = String.format("The minimum required next bid ($%.2f) exceeds your budget limit of $%.2f.", proposedBid, budget);
            }
        }

        CopilotBidProposal proposal = new CopilotBidProposal();
        proposal.setListingId(listing.getId());
        proposal.setListingTitle(listing.getTitle());
        proposal.setCurrentPrice(currentBid);
        proposal.setMinBidIncrement(increment);
        proposal.setMinNextBid(minNextBid);
        proposal.setProposedBid(proposedBid);
        proposal.setMaxBudget(budget);
        proposal.setRequiresConfirmation(true); // Mandatory security rule
        proposal.setCanAfford(canAfford);
        proposal.setWarning(warning);

        String replyText;
        if (!canAfford) {
            replyText = String.format("""
### ⚠️ Budget Exceeded for LOT #%03d (%s)

- **Current Leading Bid:** $%.2f
- **Minimum Valid Next Bid:** **$%.2f**
- **Your Specified Budget:** $%.2f

The minimum valid next bid of **$%.2f** exceeds your maximum budget of **$%.2f**. I cannot recommend bidding above your budget. Consider saving your funds or exploring other auctions!
""",
                    listing.getId(), listing.getTitle(), currentBid, minNextBid, budget, minNextBid, budget);
        } else {
            replyText = String.format("""
### 💡 Bid Recommendation for LOT #%03d: %s

- **Current Leading Bid:** $%.2f
- **Minimum Bid Step:** +$%.2f
- **Recommended Next Bid:** **$%.2f** %s
%s
> 🔒 **Mandatory Confirmation**: CampusBid fair auction policy requires explicit student authorization. Review the proposal below and click **Confirm & Place Bid** to submit.
""",
                    listing.getId(),
                    listing.getTitle(),
                    currentBid,
                    increment,
                    proposedBid,
                    budget != null ? String.format("(Within your $%.2f budget)", budget) : "",
                    budget != null ? String.format("- **Max Agreed Budget:** $%.2f\n", budget) : ""
            );
        }

        CopilotChatResponse response = new CopilotChatResponse(replyText, "BID_ASSISTANCE");
        response.setMatchingListings(List.of(listing));
        response.setBidProposal(proposal);

        if (canAfford) {
            response.setSuggestedActions(List.of(
                    String.format("Confirm $%.2f on Lot #%d", proposedBid, listing.getId()),
                    "Change my budget",
                    "Show other active auctions"
            ));
        }

        return response;
    }

    private CopilotChatResponse handleSearchAndRecommendations(String rawMessage, String lower) {
        String category = extractCategory(lower);
        ItemCondition condition = extractCondition(lower);
        BigDecimal maxPrice = extractPrice(rawMessage);
        Boolean closingSoon = isClosingSoon(lower);
        String keyword = extractSearchKeyword(lower);

        // Query active listings from MySQL using real database data
        List<ListingResponse> matches = listingService.getActiveListings(
                category,
                condition,
                null,
                maxPrice,
                closingSoon,
                keyword,
                closingSoon != null && closingSoon ? "closingSoon" : "newest"
        );

        CopilotChatResponse response = new CopilotChatResponse();
        response.setIntent("SEARCH");

        if (matches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("### 🔍 No Matching Auctions Found\n\n");
            sb.append("I searched our active campus catalog with the following filters:\n");
            if (keyword != null && !keyword.isBlank()) sb.append(String.format("- Keyword: **\"%s\"**\n", keyword));
            if (category != null) sb.append(String.format("- Category: **%s**\n", category));
            if (maxPrice != null) sb.append(String.format("- Max Price: **₹%,.2f** ($%,.2f)\n", maxPrice, maxPrice));
            if (condition != null) sb.append(String.format("- Condition: **%s**\n", condition));
            if (closingSoon != null && closingSoon) sb.append("- Closing Deadline: **Ending within 24 hours**\n");

            sb.append("\n**Suggestion:** Try relaxing your budget filter or searching in **All Categories** to see more active items.");
            response.setReply(sb.toString());
            response.setSuggestedActions(List.of(
                    "Show all active auctions",
                    "Find me a cycle under ₹2,000",
                    "Show electronics auctions closing soon",
                    "Which books are in good condition under ₹500?"
            ));
            return response;
        }

        // We have matches: build an editorial recommendation reply
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### 🎪 I Found %d Active %s for You\n\n",
                matches.size(), matches.size() == 1 ? "Auction" : "Auctions"));

        sb.append("*Here are verified active campus lots matching your request:*\n\n");

        for (int i = 0; i < Math.min(matches.size(), 3); i++) {
            ListingResponse item = matches.get(i);
            BigDecimal leading = item.getCurrentLeadingBid() != null ? item.getCurrentLeadingBid() : item.getStartingPrice();
            sb.append(String.format("**%d. LOT #%03d — %s**\n", i + 1, item.getId(), item.getTitle()));
            sb.append(String.format("   - **Current Bid:** $%.2f (Starts at $%.2f) • **Category:** %s\n",
                    leading, item.getStartingPrice(), item.getCategory()));
            sb.append(String.format("   - **Why this matches:** Fits your request in **%s** under condition `%s`.\n\n",
                    item.getCategory(), item.getCondition()));
        }

        sb.append("> ℹ️ *Note: Recommendations are live suggestions based on current bidding activity, not guaranteed final prices.*\n");

        response.setReply(sb.toString());
        response.setMatchingListings(matches);
        response.setSuggestedActions(List.of(
                String.format("Help me bid on Lot #%d", matches.get(0).getId()),
                "Show electronics auctions closing soon",
                "Explain how bidding works"
        ));

        return response;
    }

    // =========================================================================
    // Natural Language Parsing Helpers
    // =========================================================================

    private boolean isExplainRulesIntent(String lower) {
        return lower.contains("how bidding works") ||
                lower.contains("how does bidding work") ||
                lower.contains("explain bidding") ||
                lower.contains("bidding rules") ||
                lower.contains("how to bid") ||
                lower.contains("how do i bid") ||
                lower.contains("rules of auction");
    }

    private boolean isItemStatusIntent(String lower) {
        return lower.contains("current highest bid") ||
                lower.contains("highest bid") ||
                lower.contains("current bid") ||
                lower.contains("what is the bid") ||
                lower.contains("price of lot") ||
                lower.contains("status of lot");
    }

    private boolean isAskBidOnContext(String lower) {
        return lower.contains("highest bid") ||
                lower.contains("current bid") ||
                lower.contains("price") ||
                lower.contains("status");
    }

    private boolean isBidAssistanceIntent(String lower) {
        return lower.contains("help me bid") ||
                lower.contains("what should i bid") ||
                lower.contains("suggest a bid") ||
                lower.contains("suggest bid") ||
                lower.contains("decide a bid") ||
                lower.contains("bid for me") ||
                lower.contains("bid on lot");
    }

    private boolean isBidAssistanceForContext(String lower) {
        return lower.contains("bid") || lower.contains("budget");
    }

    private Long extractLotId(String text, Long fallbackContext) {
        Pattern p = Pattern.compile("(?i)(?:lot|item|listing)\\s*#?\\s*(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallbackContext;
    }

    private BigDecimal extractPrice(String text) {
        // Matches e.g. "under ₹2,000", "under 2000", "below $500", "₹500", "$2,000", "budget 300"
        Pattern p = Pattern.compile("(?i)(?:under|below|less than|max|budget)?\\s*[₹$]?\\s*((?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?)\\s*(?:rs|inr|usd|bucks)?");
        Matcher m = p.matcher(text);

        BigDecimal found = null;
        while (m.find()) {
            String valStr = m.group(1);
            if (valStr != null && !valStr.isBlank()) {
                try {
                    String clean = valStr.replace(",", "");
                    BigDecimal num = new BigDecimal(clean);
                    // Filter out small lot numbers (e.g. Lot #1) if not preceded by currency or price keyword
                    String matchSnippet = m.group(0).toLowerCase();
                    if (matchSnippet.contains("under") || matchSnippet.contains("below") || matchSnippet.contains("less")
                            || matchSnippet.contains("max") || matchSnippet.contains("budget")
                            || matchSnippet.contains("₹") || matchSnippet.contains("$")
                            || matchSnippet.contains("rs") || matchSnippet.contains("inr")
                            || num.compareTo(new BigDecimal("10")) > 0) {
                        found = num;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return found;
    }

    private String extractCategory(String lower) {
        if (lower.contains("cycle") || lower.contains("bike") || lower.contains("bicycle") || lower.contains("transport") || lower.contains("scooter")) {
            return "Bikes & Transport";
        }
        if (lower.contains("book") || lower.contains("textbook") || lower.contains("algorithm") || lower.contains("calculus") || lower.contains("physics")) {
            return "Textbooks";
        }
        if (lower.contains("electronic") || lower.contains("laptop") || lower.contains("macbook") || lower.contains("calculator") || lower.contains("headphone") || lower.contains("speaker") || lower.contains("keyboard") || lower.contains("phone")) {
            return "Electronics";
        }
        if (lower.contains("furniture") || lower.contains("dorm") || lower.contains("fridge") || lower.contains("lamp") || lower.contains("chair") || lower.contains("desk") || lower.contains("table") || lower.contains("bed")) {
            return "Dorm & Furniture";
        }
        if (lower.contains("cloth") || lower.contains("apparel") || lower.contains("hoodie") || lower.contains("jacket") || lower.contains("shirt")) {
            return "Apparel";
        }
        if (lower.contains("sport") || lower.contains("hobby") || lower.contains("guitar") || lower.contains("badminton") || lower.contains("racket") || lower.contains("cricket")) {
            return "Sports & Hobbies";
        }
        return null;
    }

    private ItemCondition extractCondition(String lower) {
        if (lower.contains("brand new") || lower.contains("unopened") || lower.contains("mint")) {
            return ItemCondition.NEW;
        }
        if (lower.contains("like new")) {
            return ItemCondition.LIKE_NEW;
        }
        if (lower.contains("good condition") || lower.contains("in good condition") || (lower.contains("good") && !lower.contains("goodbye"))) {
            return ItemCondition.GOOD;
        }
        if (lower.contains("fair condition") || lower.contains("fair")) {
            return ItemCondition.FAIR;
        }
        if (lower.contains("poor") || lower.contains("as-is") || lower.contains("vintage")) {
            return ItemCondition.POOR;
        }
        return null;
    }

    private Boolean isClosingSoon(String lower) {
        return lower.contains("closing soon") ||
                lower.contains("ending soon") ||
                lower.contains("ending today") ||
                lower.contains("closes soon") ||
                lower.contains("ending 24h") ||
                lower.contains("ending soonest");
    }

    private String extractSearchKeyword(String lower) {
        // Strip out common filler words, question words, prepositions, stop words
        String cleaned = lower
                .replaceAll("(?i)\\b(find me a|find me|show me|search for|search|which|what are|what is|what|who|where|how|any|under|below|less than|max|budget|closing soon|in good condition|in fair condition|like new|brand new|good condition|fair condition|condition|auctions|auction|items|item|lot|lots|for sale|please|is|are|am|in|for|with|and|or|the|a|an|of|to|at|on|by|from|about|me|i|we|you|they|there|here)\\b", " ")
                .replaceAll("[₹$0-9,]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank() || cleaned.length() <= 2) {
            return null;
        }

        // If the query contains generic category words, remove them so category filtering handles it
        String strippedCategories = cleaned.replaceAll("(?i)\\b(cycles?|bikes?|bicycles?|books?|textbooks?|electronics?|gadgets?|furniture)\\b", " ").replaceAll("\\s+", " ").trim();
        if (strippedCategories.isBlank() || strippedCategories.length() <= 2) {
            return null;
        }

        return strippedCategories;
    }
}
