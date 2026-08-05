package com.aliCheikh.stock.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record StockReceiptImportExecutionReport(
        UUID importId,
        List<StockReceiptImportRowExecutionResult> rows,
        StockReceiptImportExecutionSummary summary
) {
    public StockReceiptImportExecutionReport {
        Objects.requireNonNull(importId, "importId cannot be null");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows cannot be null"));
        Objects.requireNonNull(summary, "summary cannot be null");
        if (summary.totalRows() != rows.size()) {
            throw new IllegalArgumentException("summary totalRows must match rows size");
        }
    }
}
