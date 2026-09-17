package com.aliCheikh.stock.domain.event;

import java.time.LocalDateTime;
/** In-process domain event with its occurrence timestamp. */
public interface DomainEvent {
    LocalDateTime getOccurredAt();
}
