package com.aliCheikh.stock.application.dto;

public record StockReceiptImportSummary(
        int totalRows,
        int productsToCreate,
        int existingProductsToReceive,
        int invalidRows
) {
    public StockReceiptImportSummary {
        if (totalRows < 0 || productsToCreate < 0 || existingProductsToReceive < 0 || invalidRows < 0) {
            throw new IllegalArgumentException("summary counts cannot be negative");
        }
        if (productsToCreate + existingProductsToReceive + invalidRows != totalRows) {
            throw new IllegalArgumentException("summary counts must equal totalRows");
        }
    }
}
