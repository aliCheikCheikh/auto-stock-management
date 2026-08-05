package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record StockReceiptImportExecutionReportResponse(
        UUID importId,
        List<StockReceiptImportRowExecutionResponse> rows,
        StockReceiptImportExecutionSummaryResponse summary
) {
}
