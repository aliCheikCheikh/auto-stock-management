package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record CreateSaleLine(UUID productId,
                             int quantity) {
}
