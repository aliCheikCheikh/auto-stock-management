package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementId;

import java.time.Instant;

public record TransferStockResult(
        MovementId movementId,
        Instant acceptedAt
) {
}
