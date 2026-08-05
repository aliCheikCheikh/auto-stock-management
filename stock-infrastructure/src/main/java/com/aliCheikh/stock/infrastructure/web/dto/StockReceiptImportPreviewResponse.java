package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

public record StockReceiptImportPreviewResponse(
        List<StockReceiptImportRowPreviewResponse> rows,
        StockReceiptImportSummaryResponse summary
) {
}
