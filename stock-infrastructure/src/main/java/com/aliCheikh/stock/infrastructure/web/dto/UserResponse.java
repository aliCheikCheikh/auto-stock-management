package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record UserResponse(UUID userId,
                           String email,
                           String role) {
}
