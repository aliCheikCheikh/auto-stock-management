package com.aliCheikh.stock.domain.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    public LocalDateTime getOccurredAt();
}
