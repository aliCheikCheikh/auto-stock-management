package com.aliCheikh.stock.domain.event;

import java.time.LocalDateTime;
/**
 * Represents a domain event.
 *
 * TODO (Architecture): Before moving to asynchronous messaging (Kafka/RabbitMQ),
 * add 'eventId' (UUID) and 'eventVersion' (int) to allow for deduplication and tracing.
 */
public interface DomainEvent {
    LocalDateTime getOccurredAt();
}
