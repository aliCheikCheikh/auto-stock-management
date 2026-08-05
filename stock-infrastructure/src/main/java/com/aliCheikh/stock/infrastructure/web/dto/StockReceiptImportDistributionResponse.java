package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record StockReceiptImportDistributionResponse(
        UUID locationId,
        int quantity
) {
}
