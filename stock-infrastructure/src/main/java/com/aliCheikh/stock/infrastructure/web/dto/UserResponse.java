package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.user.User;

import java.util.UUID;

public record UserResponse(UUID userId,
                           String displayName,
                           String email,
                           String role,
                           boolean passwordChangeRequired,
                           boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().getValue(),
                user.getDisplayName(),
                user.getEmail().getValue(),
                user.getRole().name(),
                user.isPasswordChangeRequired(),
                user.isActive());
    }
}
