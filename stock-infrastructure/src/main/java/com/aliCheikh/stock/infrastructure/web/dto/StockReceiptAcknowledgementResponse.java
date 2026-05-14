package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReceiptAcknowledgementResponse(
        UUID productId,
        int totalReceived,
        Instant acceptedAt,
        List<UUID> movementIds
) {
}
