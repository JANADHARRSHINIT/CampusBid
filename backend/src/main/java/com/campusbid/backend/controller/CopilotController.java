package com.campusbid.backend.controller;

import com.campusbid.backend.dto.*;
import com.campusbid.backend.service.CopilotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/copilot")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    /**
     * Chat with the AI Auction Copilot.
     * Parses natural-language search, recommendations, status inquiries, rules explanation, and bid assistance.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<CopilotChatResponse>> chat(
            @Valid @RequestBody CopilotChatRequest request,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        CopilotChatResponse response = copilotService.processChat(request, studentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Copilot response generated successfully",
                response
        ));
    }

    /**
     * Confirms and places a proposed bid with mandatory explicit student confirmation.
     * Enforces authentication, student budget cap, and core bidding rules.
     */
    @PostMapping("/confirm-bid")
    public ResponseEntity<ApiResponse<ListingResponse>> confirmBid(
            @Valid @RequestBody CopilotConfirmBidRequest request,
            @RequestHeader(value = "X-Student-Id", required = false) String studentId) {

        ListingResponse response = copilotService.confirmBid(request, studentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Bid placed successfully via AI Copilot confirmation",
                response
        ));
    }
}
