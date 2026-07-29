package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record LoginResponse(UUID userId,
                            String displayName,
                            String email,
                            String role,
                            boolean passwordTemporary) {
}
