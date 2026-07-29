package com.aliCheikh.stock.infrastructure.web.dto;

public record CreatedUserResponse(UserResponse user, String temporaryPassword) {

    @Override
    public String toString() {
        return "CreatedUserResponse[user=" + user + ", temporaryPassword=[REDACTED]]";
    }
}
