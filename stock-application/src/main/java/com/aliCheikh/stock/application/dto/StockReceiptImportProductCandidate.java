package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.Objects;

public record StockReceiptImportProductCandidate(
        ProductId productId,
        String reference,
        String name,
        boolean active
) {
    public StockReceiptImportProductCandidate {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
    }
}
