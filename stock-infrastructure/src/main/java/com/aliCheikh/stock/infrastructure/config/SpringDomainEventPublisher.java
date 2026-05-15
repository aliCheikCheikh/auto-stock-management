package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class SpringDomainEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher cannot be null"
        );
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events cannot be null")
                .forEach(applicationEventPublisher::publishEvent);
    }
}
