package com.campusbid.backend.dto;

public class CancelSaleRequest {
    private String reason;

    public CancelSaleRequest() {}

    public CancelSaleRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
