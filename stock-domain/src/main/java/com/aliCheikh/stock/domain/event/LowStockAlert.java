package com.aliCheikh.stock.domain.event;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Émis lorsque le stock global d'un produit tombe en dessous de son seuil d'alerte.
 */
public record LowStockAlert(
        ProductId productId,
        String productName,    // Ajouté selon la spec
        int globalQuantity,    // Renommé (anciennement currentQuantity)
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