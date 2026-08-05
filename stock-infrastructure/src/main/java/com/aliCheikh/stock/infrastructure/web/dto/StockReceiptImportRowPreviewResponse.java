package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record StockReceiptImportRowPreviewResponse(
        int lineNumber,
        String action,
        String reference,
        String name,
        String categoryName,
        UUID categoryId,
        MoneyResponse unitPrice,
        Integer minimumGlobalThreshold,
        UUID productId,
        List<StockReceiptImportDistributionResponse> distributions,
        List<StockReceiptImportIssueResponse> issues
) {
}
