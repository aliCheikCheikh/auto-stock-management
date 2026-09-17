package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.List;
import java.util.Objects;

public record StockReceiptImportRowExecutionResult(
        int lineNumber,
        StockReceiptImportRowExecutionStatus status,
        StockReceiptImportRowAction action,
        String reference,
        String name,
        ProductId productId,
        int quantityReceived,
        List<StockReceiptImportIssue> issues
) {
    public StockReceiptImportRowExecutionResult {
        if (lineNumber < 2) {
            throw new IllegalArgumentException("lineNumber must point after the header");
        }
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues cannot be null"));
        if (quantityReceived < 0) {
            throw new IllegalArgumentException("quantityReceived cannot be negative");
        }
        validateState(status, action, productId, quantityReceived, issues);
    }

    public static StockReceiptImportRowExecutionResult imported(
            int lineNumber,
            StockReceiptImportRowAction action,
            String reference,
            String name,
            ProductId productId,
            int quantityReceived
    ) {
        return new StockReceiptImportRowExecutionResult(
                lineNumber, StockReceiptImportRowExecutionStatus.IMPORTED, action,
                reference, name, productId, quantityReceived, List.of());
    }

    public static StockReceiptImportRowExecutionResult ignoredInvalid(StockReceiptImportRowPreview row) {
        return new StockReceiptImportRowExecutionResult(
                row.lineNumber(), StockReceiptImportRowExecutionStatus.IGNORED_INVALID, row.action(),
                row.reference(), row.name(), null, 0, row.issues());
    }

    public static StockReceiptImportRowExecutionResult ignoredByUser(StockReceiptImportRowPreview row) {
        return new StockReceiptImportRowExecutionResult(
                row.lineNumber(), StockReceiptImportRowExecutionStatus.IGNORED_BY_USER, row.action(),
                row.reference(), row.name(), null, 0, List.of());
    }

    public static StockReceiptImportRowExecutionResult failed(StockReceiptImportRowPreview row) {
        return new StockReceiptImportRowExecutionResult(
                row.lineNumber(), StockReceiptImportRowExecutionStatus.FAILED, row.action(),
                row.reference(), row.name(), null, 0,
                List.of(new StockReceiptImportIssue(
                        "ligne",
                        StockReceiptImportIssueCode.EXECUTION_FAILED,
                        "This row could not be imported. Generate a new preview before retrying."
                )));
    }

    private static void validateState(
            StockReceiptImportRowExecutionStatus status,
            StockReceiptImportRowAction action,
            ProductId productId,
            int quantityReceived,
            List<StockReceiptImportIssue> issues
    ) {
        switch (status) {
            case IMPORTED -> {
                if (action == StockReceiptImportRowAction.REJECT
                        || productId == null || quantityReceived <= 0 || !issues.isEmpty()) {
                    throw new IllegalArgumentException("an imported row must contain its successful receipt");
                }
            }
            case IGNORED_INVALID -> {
                if (action != StockReceiptImportRowAction.REJECT
                        || productId != null || quantityReceived != 0 || issues.isEmpty()) {
                    throw new IllegalArgumentException("an invalid row must keep its validation issues");
                }
            }
            case IGNORED_BY_USER -> {
                if (action == StockReceiptImportRowAction.REJECT
                        || productId != null || quantityReceived != 0 || !issues.isEmpty()) {
                    throw new IllegalArgumentException("a user-ignored row must have been ready to import");
                }
            }
            case FAILED -> {
                if (action == StockReceiptImportRowAction.REJECT
                        || productId != null || quantityReceived != 0 || issues.isEmpty()) {
                    throw new IllegalArgumentException("a failed row must explain its execution failure");
                }
            }
        }
    }
}
