package com.aliCheikh.stock.infrastructure.web.dto;

public record StockReceiptImportIssueResponse(
        String field,
        String code,
        String message
) {
}
