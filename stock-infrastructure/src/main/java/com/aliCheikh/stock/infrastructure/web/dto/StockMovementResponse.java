package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.movement.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID movementId,
        UUID productId,
        UUID locationId,
        UUID destinationLocationId,
        MovementType type,
        int quantity,
        UUID executedBy,
        String executedByName,
        LocalDateTime executedAt,
        UUID saleId,
        UUID operationId,
        /** Originating sale balance: null for non-sale movements, zero when settled. */
        MoneyResponse saleAmountDue
) {
}
