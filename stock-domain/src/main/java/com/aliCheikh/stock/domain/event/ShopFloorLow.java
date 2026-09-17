package com.aliCheikh.stock.domain.event;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;

import java.time.LocalDateTime;
import java.util.Objects;

/** Emitted when a SHOP_FLOOR location falls below its own low-stock threshold. */
public record ShopFloorLow(
        ProductId productId,
        LocationId locationId,
        int currentQuantity,
        int lowStockIndicator,
        LocalDateTime occurredAt
) implements DomainEvent {

    public ShopFloorLow {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(locationId, "locationId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
