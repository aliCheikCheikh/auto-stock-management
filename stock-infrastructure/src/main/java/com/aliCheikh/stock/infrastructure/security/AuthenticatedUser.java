package com.aliCheikh.stock.infrastructure.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role) {
}
