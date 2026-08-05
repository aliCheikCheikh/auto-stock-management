package com.aliCheikh.stock.infrastructure.web.dto;

public record StockReceiptImportExecutionSummaryResponse(
        int totalRows,
        int selectedRows,
        int importedRows,
        int productsCreated,
        int existingProductsReceived,
        int ignoredInvalidRows,
        int ignoredByUserRows,
        int failedRows,
        int totalQuantityReceived
) {
}
