package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record LoginResponse(UUID userId,
                            String role,
                            boolean passwordTemporary) {
}
