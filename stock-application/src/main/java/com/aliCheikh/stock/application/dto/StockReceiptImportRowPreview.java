package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.List;
import java.util.Objects;

public record StockReceiptImportRowPreview(
        int lineNumber,
        StockReceiptImportRowAction action,
        String reference,
        String name,
        String categoryName,
        CategoryId categoryId,
        Money unitPrice,
        Integer minimumGlobalThreshold,
        ProductId productId,
        List<TargetLocation> distributions,
        List<StockReceiptImportIssue> issues
) {
    public StockReceiptImportRowPreview {
        if (lineNumber < 2) {
            throw new IllegalArgumentException("lineNumber must point after the header");
        }
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(categoryName, "categoryName cannot be null");
        distributions = List.copyOf(Objects.requireNonNull(distributions, "distributions cannot be null"));
        issues = List.copyOf(Objects.requireNonNull(issues, "issues cannot be null"));
        if (action == StockReceiptImportRowAction.REJECT && issues.isEmpty()) {
            throw new IllegalArgumentException("a rejected row must explain why");
        }
        if (action != StockReceiptImportRowAction.REJECT && !issues.isEmpty()) {
            throw new IllegalArgumentException("a ready row cannot contain blocking issues");
        }
    }
}
