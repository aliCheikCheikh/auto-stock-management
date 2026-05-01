package com.aliCheikh.stock.domain.event;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public record StockReceived(
        ProductId productId,
        int totalQuantityReceived,
        Map<LocationId, Integer> locationBreakdown,
        UserId receivedBy,
        LocalDateTime occurredAt
) implements DomainEvent {
    public StockReceived {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(locationBreakdown, "locationBreakdown cannot be null");
        Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}