package com.campusbid.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class CopilotChatResponse {

    private String reply;
    private String intent;
    private List<String> suggestedActions = new ArrayList<>();
    private List<ListingResponse> matchingListings = new ArrayList<>();
    private CopilotBidProposal bidProposal;

    public CopilotChatResponse() {
    }

    public CopilotChatResponse(String reply, String intent) {
        this.reply = reply;
        this.intent = intent;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public List<ListingResponse> getMatchingListings() {
        return matchingListings;
    }

    public void setMatchingListings(List<ListingResponse> matchingListings) {
        this.matchingListings = matchingListings;
    }

    public CopilotBidProposal getBidProposal() {
        return bidProposal;
    }

    public void setBidProposal(CopilotBidProposal bidProposal) {
        this.bidProposal = bidProposal;
    }
}
