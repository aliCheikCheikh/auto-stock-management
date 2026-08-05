package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record StockReceiptImportRowExecutionResponse(
        int lineNumber,
        String status,
        String action,
        String reference,
        String name,
        UUID productId,
        int quantityReceived,
        List<StockReceiptImportIssueResponse> issues
) {
}
