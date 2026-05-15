package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TransferStockRequest(
        @NotNull UUID productId,
        @NotNull UUID sourceLocationId,
        @NotNull UUID destinationLocationId,
        @Positive int quantity,
        @NotNull UUID userId
) {
}
