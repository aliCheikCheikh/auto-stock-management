package com.aliCheikh.stock.infrastructure.web.dto;

public record StockReceiptImportSummaryResponse(
        int totalRows,
        int productsToCreate,
        int existingProductsToReceive,
        int invalidRows
) {
}
