package com.campusbid.backend.model;

/**
 * Types of notification events in CampusBid lifecycle.
 */
public enum NotificationType {
    OUTBID,              // Buyer: Someone outbids you
    BID_ACCEPTED,        // Buyer: Your bid is accepted as the leading bid
    AUCTION_WON,         // Buyer: You won an auction
    BID_APPROVED,        // Buyer: Seller approved your winning bid
    BID_REJECTED,        // Buyer: Seller rejected your winning bid
    BID_RECEIVED,        // Seller: Someone placed a bid on your item
    AUCTION_CLOSED,      // Seller: Your auction closed
    AWAITING_APPROVAL,   // Seller: Winning bid is waiting for your approval
    HANDOVER_CONFIRMED,  // Either party: Campus handover confirmed by counterpart
    SALE_COMPLETED,      // Both parties: Campus exchange confirmed and sale completed
    SALE_CANCELLED       // Either party: Sale cancelled or failed pickup recorded
}
