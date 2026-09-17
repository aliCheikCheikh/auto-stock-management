package com.aliCheikh.stock.domain.event;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.time.LocalDateTime;
import java.util.Objects;

/** Emitted when a product's total stock falls below its alert threshold. */
public record LowStockAlert(
        ProductId productId,
        String productName,
        int globalQuantity,
        int threshold,
        LocalDateTime occurredAt
) implements DomainEvent {

    public LowStockAlert {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(productName, "productName cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
