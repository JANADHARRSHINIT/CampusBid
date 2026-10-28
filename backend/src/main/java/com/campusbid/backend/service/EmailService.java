package com.campusbid.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${campusbid.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${campusbid.mail.from:noreply@campusbid.edu}")
    private String mailFrom;

    @Value("${campusbid.app.base-url:http://localhost:5174}")
    private String appBaseUrl;

    /**
     * Send email notification asynchronously.
     * Guaranteed never to fail or roll back business transactions if SMTP fails.
     */
    @Async
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String htmlContent) {
        if (!mailEnabled || mailSender == null) {
            log.info("Email notification simulated [mailEnabled={}, to='{}', subject='{}']",
                    mailEnabled, to, subject);
            return CompletableFuture.completedFuture(true);
        }

        if (to == null || to.isBlank()) {
            log.warn("Cannot send email: recipient address is empty.");
            return CompletableFuture.completedFuture(false);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom, "CampusBid Flea Market");
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email delivered successfully [to='{}', subject='{}']", to, subject);
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.warn("Failed to deliver email [to='{}', subject='{}']: {}", to, subject, ex.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send Outbid email to a previous leading bidder.
     */
    public void sendOutbidEmail(String buyerEmail, String buyerName, String lotTitle, Long lotId, BigDecimal newPrice) {
        String subject = "⚡ You've been outbid on: " + lotTitle;
        String priceDisplay = newPrice != null ? ("$" + newPrice) : "a higher amount";
        String content = buildHtmlTemplate(
                "You've Been Outbid!",
                "Hello " + (buyerName != null ? buyerName : "Student") + ",",
                "Another student has just placed a higher bid of <strong>" + priceDisplay + "</strong> on <em>" + lotTitle + "</em>.",
                "To remain in the lead, submit a counter-bid before the auction closes.",
                lotId,
                "View Auction & Place Counter-Bid"
        );
        sendEmailAsync(buyerEmail, subject, content);
    }

    /**
     * Send Bid Accepted email to the new leading bidder.
     */
    public void sendBidAcceptedEmail(String buyerEmail, String buyerName, String lotTitle, Long lotId, BigDecimal amount) {
        String priceDisplay = amount != null ? ("$" + amount) : "your bid amount";
        String subject = "✅ Your bid of " + priceDisplay + " is now leading on: " + lotTitle;
        String content = buildHtmlTemplate(
                "Your Bid is in the Lead!",
                "Hello " + (buyerName != null ? buyerName : "Student") + ",",
                "Great news! Your bid of <strong>" + priceDisplay + "</strong> on <em>" + lotTitle + "</em> was accepted and is currently the leading bid.",
                "We will notify you immediately if anyone places a higher bid or when the auction reaches its deadline.",
                lotId,
                "Check Live Auction Status"
        );
        sendEmailAsync(buyerEmail, subject, content);
    }

    /**
     * Send Bid Received email to the seller.
     */
    public void sendBidReceivedEmail(String sellerEmail, String sellerName, String lotTitle, Long lotId, BigDecimal amount, String bidderName) {
        String priceDisplay = amount != null ? ("$" + amount) : "a new amount";
        String subject = "🏷️ New Bid Received (" + priceDisplay + ") on your listing: " + lotTitle;
        String content = buildHtmlTemplate(
                "New Bid on Your Lot",
                "Hello " + (sellerName != null ? sellerName : "Seller") + ",",
                (bidderName != null ? bidderName : "A student") + " just placed a new bid of <strong>" + priceDisplay + "</strong> on your consigned item: <em>" + lotTitle + "</em>.",
                "Bidding remains active until the scheduled auction closing deadline.",
                lotId,
                "View Your Listing"
        );
        sendEmailAsync(sellerEmail, subject, content);
    }

    /**
     * Send Auction Won / Provisional Winner email.
     */
    public void sendAuctionWonEmail(String buyerEmail, String buyerName, String lotTitle, Long lotId, BigDecimal winningAmount) {
        String subject = "🏆 Congratulations! You won the auction for: " + lotTitle;
        String priceDisplay = winningAmount != null ? ("$" + winningAmount) : "your winning bid";
        String content = buildHtmlTemplate(
                "You're the Winning Bidder!",
                "Hello " + (buyerName != null ? buyerName : "Student") + ",",
                "The auction for <em>" + lotTitle + "</em> has closed, and your bid of <strong>" + priceDisplay + "</strong> is the winning bid!",
                "The seller is currently reviewing the sale. Once approved, you can coordinate the physical on-campus exchange.",
                lotId,
                "View Won Auction & Sale Details"
        );
        sendEmailAsync(buyerEmail, subject, content);
    }

    /**
     * Send Auction Closed email to seller.
     */
    public void sendAuctionClosedEmail(String sellerEmail, String sellerName, String lotTitle, Long lotId, boolean hasWinner, BigDecimal topBid) {
        String subject = "⌛ Your auction has closed: " + lotTitle;
        String priceDisplay = topBid != null ? ("$" + topBid) : "the top amount";
        String body = hasWinner
                ? "Your auction concluded with a top winning bid of <strong>" + priceDisplay + "</strong>. Please visit your Seller Decision Station to APPROVE or REJECT the winning bid."
                : "Your auction closed without receiving any bids. You may re-list or adjust the starting price at any time.";

        String content = buildHtmlTemplate(
                "Auction Closed",
                "Hello " + (sellerName != null ? sellerName : "Seller") + ",",
                body,
                hasWinner ? "Prompt approval ensures smooth coordination with the buyer." : "Thank you for using CampusBid.",
                lotId,
                hasWinner ? "Review Winning Bid" : "View Listing"
        );
        sendEmailAsync(sellerEmail, subject, content);
    }

    /**
     * Send Bid Approved email to buyer.
     */
    public void sendBidApprovedEmail(String buyerEmail, String buyerName, String lotTitle, Long lotId, BigDecimal finalAmount) {
        String subject = "🎉 Sale Approved! Coordinate pickup for: " + lotTitle;
        String priceDisplay = finalAmount != null ? ("$" + finalAmount) : "the winning amount";
        String content = buildHtmlTemplate(
                "Seller Approved Your Winning Bid!",
                "Hello " + (buyerName != null ? buyerName : "Student") + ",",
                "The seller has approved your winning bid of <strong>" + priceDisplay + "</strong> for <em>" + lotTitle + "</em>. The item is now reserved exclusively for you.",
                "Please arrange a safe, well-lit campus meeting point (e.g., Student Union, Library) for physical inspection and cash/UPI handover.",
                lotId,
                "View Sale & Handover Instructions"
        );
        sendEmailAsync(buyerEmail, subject, content);
    }

    /**
     * Send Bid Rejected email to buyer.
     */
    public void sendBidRejectedEmail(String buyerEmail, String buyerName, String lotTitle, Long lotId) {
        String subject = "Notice regarding closed auction: " + lotTitle;
        String content = buildHtmlTemplate(
                "Winning Bid Decision Update",
                "Hello " + (buyerName != null ? buyerName : "Student") + ",",
                "The seller decided not to proceed with the winning bid on <em>" + lotTitle + "</em>. This auction has concluded.",
                "We invite you to explore other authentic lots currently live in our collegiate flea market catalog.",
                lotId,
                "Explore Other Campus Auctions"
        );
        sendEmailAsync(buyerEmail, subject, content);
    }

    /**
     * Send Handover Confirmed / Sale Completed email.
     */
    public void sendSaleCompletedEmail(String recipientEmail, String recipientName, String lotTitle, Long lotId, boolean isSeller) {
        String subject = "🤝 Handover Confirmed: " + lotTitle;
        String content = buildHtmlTemplate(
                "Sale Complete!",
                "Hello " + (recipientName != null ? recipientName : "Student") + ",",
                "Physical item handover has been successfully verified for <em>" + lotTitle + "</em>.",
                isSeller
                        ? "Thank you for selling through CampusBid! Your transaction is complete."
                        : "Enjoy your purchase! Thank you for participating in the CampusBid student community.",
                lotId,
                "View Completed Lot Record"
        );
        sendEmailAsync(recipientEmail, subject, content);
    }

    /**
     * Send Wanted Item Smart Alert email when a matching listing is posted.
     */
    public void sendWantedItemMatchEmail(String recipientEmail, String recipientName, String lotTitle, Long lotId, BigDecimal price) {
        String formattedPrice = price != null ? ("₹" + price.toPlainString()) : "a great price";
        String subject = "🎯 Wanted Item Available: " + lotTitle;
        String content = buildHtmlTemplate(
                "Your Wanted Item Is Available!",
                "Hello " + (recipientName != null ? recipientName : "Student") + ",",
                "Great news! A classmate has listed an item matching your Wanted Item request: <strong>" + lotTitle + "</strong>.",
                "Starting Price: <strong>" + formattedPrice + "</strong>. This item is active and open for bidding now.",
                lotId,
                "View Auction Lot"
        );
        sendEmailAsync(recipientEmail, subject, content);
    }

    /**
     * Send Test Email for administrative diagnostics.
     */
    public boolean sendTestEmail(String targetEmail) {
        String subject = "🧪 CampusBid Test Email Notification";
        String content = buildHtmlTemplate(
                "SMTP Test Connection Successful",
                "Hello Administrator,",
                "This is a test notification confirming that Spring Boot JavaMailSender and SMTP configuration are operating properly on CampusBid.",
                "All student auction notification templates (outbid, winning bid, seller approvals) are configured and ready for live dispatch.",
                null,
                "Open Campus Marketplace"
        );
        try {
            return sendEmailAsync(targetEmail, subject, content).get();
        } catch (Exception e) {
            log.warn("Test email dispatch threw exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper to render mobile-responsive, modern HTML email templates.
     */
    private String buildHtmlTemplate(String headerTitle, String greeting, String mainText, String subText, Long lotId, String buttonText) {
        String actionUrl = lotId != null ? (appBaseUrl + "/#lot-" + lotId) : appBaseUrl;

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #FAF7F2; margin: 0; padding: 20px; color: #2C2825; }" +
                ".container { max-width: 560px; margin: 0 auto; background: #FFFFFF; border-radius: 12px; border: 1px solid #E5DFD5; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.04); }" +
                ".header { background: #1C4D35; padding: 24px 30px; text-align: left; }" +
                ".header h1 { color: #FFFFFF; margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.3px; }" +
                ".header span { color: #F5E6CC; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }" +
                ".content { padding: 30px; }" +
                ".title { font-size: 18px; font-weight: 700; color: #1C4D35; margin-top: 0; margin-bottom: 12px; }" +
                ".greeting { font-size: 15px; font-weight: 600; margin-bottom: 14px; color: #333; }" +
                ".body-text { font-size: 14px; line-height: 1.6; color: #4A453F; margin-bottom: 16px; }" +
                ".sub-text { font-size: 13px; line-height: 1.5; color: #736B63; margin-bottom: 24px; padding: 12px; background: #F8F5F0; border-radius: 8px; border-left: 3px solid #E65100; }" +
                ".btn { display: inline-block; background-color: #1C4D35; color: #FFFFFF !important; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: 600; font-size: 14px; text-align: center; }" +
                ".footer { background: #F4EFEA; padding: 18px 30px; text-align: center; font-size: 12px; color: #8C827A; border-top: 1px solid #E5DFD5; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <span>Collegiate Flea Market & Auction Exchange</span>" +
                "    <h1>CampusBid Notification</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <h2 class='title'>" + headerTitle + "</h2>" +
                "    <p class='greeting'>" + greeting + "</p>" +
                "    <p class='body-text'>" + mainText + "</p>" +
                "    <p class='sub-text'>" + subText + "</p>" +
                "    <div style='text-align: center; margin: 28px 0;'>" +
                "      <a href='" + actionUrl + "' class='btn' target='_blank'>" + buttonText + " &rarr;</a>" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; 2026 CampusBid • Verified Student Accounts • Safe On-Campus In-Person Exchange" +
                "  </div>" +
                "</div>" +
                "</body></html>";
    }
}
