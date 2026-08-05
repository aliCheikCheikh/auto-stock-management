package com.aliCheikh.stock.application.dto;

import java.util.Objects;

public record StockReceiptImportIssue(
        String field,
        StockReceiptImportIssueCode code,
        String message
) {
    public StockReceiptImportIssue {
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }
}
