package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record StockTransferAcknowledgementResponse(
        UUID movementId,
        Instant acceptedAt
) {
}
