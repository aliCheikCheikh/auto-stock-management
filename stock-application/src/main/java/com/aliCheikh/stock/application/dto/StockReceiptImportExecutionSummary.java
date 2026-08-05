package com.aliCheikh.stock.application.dto;

import java.util.List;

public record StockReceiptImportExecutionSummary(
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
    public StockReceiptImportExecutionSummary {
        if (totalRows < 0 || selectedRows < 0 || importedRows < 0 || productsCreated < 0
                || existingProductsReceived < 0 || ignoredInvalidRows < 0 || ignoredByUserRows < 0
                || failedRows < 0 || totalQuantityReceived < 0) {
            throw new IllegalArgumentException("execution summary values cannot be negative");
        }
        if (selectedRows > totalRows) {
            throw new IllegalArgumentException("selectedRows cannot exceed totalRows");
        }
        if (productsCreated + existingProductsReceived != importedRows) {
            throw new IllegalArgumentException("imported action counts must equal importedRows");
        }
        if (importedRows + ignoredInvalidRows + ignoredByUserRows + failedRows != totalRows) {
            throw new IllegalArgumentException("row status counts must equal totalRows");
        }
    }

    public static StockReceiptImportExecutionSummary from(
            List<StockReceiptImportRowExecutionResult> rows,
            int selectedRows
    ) {
        int imported = countStatus(rows, StockReceiptImportRowExecutionStatus.IMPORTED);
        int productsCreated = countImportedAction(rows, StockReceiptImportRowAction.CREATE_PRODUCT);
        int existingReceived = countImportedAction(rows, StockReceiptImportRowAction.RECEIVE_EXISTING);
        int ignoredInvalid = countStatus(rows, StockReceiptImportRowExecutionStatus.IGNORED_INVALID);
        int ignoredByUser = countStatus(rows, StockReceiptImportRowExecutionStatus.IGNORED_BY_USER);
        int failed = countStatus(rows, StockReceiptImportRowExecutionStatus.FAILED);
        int quantity = rows.stream().mapToInt(StockReceiptImportRowExecutionResult::quantityReceived).sum();
        return new StockReceiptImportExecutionSummary(
                rows.size(), selectedRows, imported, productsCreated, existingReceived,
                ignoredInvalid, ignoredByUser, failed, quantity);
    }

    private static int countStatus(
            List<StockReceiptImportRowExecutionResult> rows,
            StockReceiptImportRowExecutionStatus status
    ) {
        return (int) rows.stream().filter(row -> row.status() == status).count();
    }

    private static int countImportedAction(
            List<StockReceiptImportRowExecutionResult> rows,
            StockReceiptImportRowAction action
    ) {
        return (int) rows.stream()
                .filter(row -> row.status() == StockReceiptImportRowExecutionStatus.IMPORTED)
                .filter(row -> row.action() == action)
                .count();
    }
}
