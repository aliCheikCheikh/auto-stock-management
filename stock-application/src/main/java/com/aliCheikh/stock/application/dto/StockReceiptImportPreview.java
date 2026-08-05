package com.aliCheikh.stock.application.dto;

import java.util.List;
import java.util.Objects;

public record StockReceiptImportPreview(
        List<StockReceiptImportRowPreview> rows,
        StockReceiptImportSummary summary
) {
    public StockReceiptImportPreview {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows cannot be null"));
        Objects.requireNonNull(summary, "summary cannot be null");
    }
}
