package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateSaleLine(UUID productId,
                             @Positive int quantity) {
}
