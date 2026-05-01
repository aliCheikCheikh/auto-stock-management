package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.domain.event.DomainEvent;

import java.util.List;

public interface EventPublisher {
    public void publish(List<DomainEvent> events);
}
