package com.aliCheikh.stock.domain.event;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

public record SaleCompleted(
        SaleId saleId,
        Money totalAmount,
        int lineItemCount,
        UserId soldBy,
        LocalDateTime occurredAt
) implements DomainEvent {
    public SaleCompleted {
        Objects.requireNonNull(saleId, "saleId cannot be null");
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        Objects.requireNonNull(soldBy, "soldBy cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}