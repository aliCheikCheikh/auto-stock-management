package com.aliCheikh.stock.infrastructure.web.dto;

public record TemporaryPasswordResponse(String temporaryPassword) {

    @Override
    public String toString() {
        return "TemporaryPasswordResponse[temporaryPassword=[REDACTED]]";
    }
}
