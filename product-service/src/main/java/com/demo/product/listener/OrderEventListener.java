package com.demo.product.listener;

import com.demo.events.order.OrderConfirmedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.product.entity.ProcessedEvent;
import com.demo.product.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;

    public OrderEventListener(ProcessedEventRepository processedEventRepository, MeterRegistry meterRegistry) {
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "order.created", groupId = "product-service")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (isDuplicate(event.eventId(), "OrderCreatedEvent")) {
            return;
        }
        markProcessed(event.eventId(), "OrderCreatedEvent");
        logger.info("Received OrderCreated: key={}, total={}, occurredAt={}", event.key(), event.totalAmount(),
                event.occurredAt());
        meterRegistry.counter("order.event.consumed", "service", "product-service", "event", "order-created")
                .increment();
    }

    @KafkaListener(topics = "order.confirmed", groupId = "product-service")
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (isDuplicate(event.eventId(), "OrderConfirmedEvent")) {
            return;
        }
        markProcessed(event.eventId(), "OrderConfirmedEvent");
        logger.info("Received OrderConfirmed: key={}, createdAt={}", event.key(), event.occurredAt());
        meterRegistry.counter("order.event.consumed", "service", "product-service", "event", "order-confirmed")
                .increment();
    }

    private boolean isDuplicate(UUID eventId, String eventType) {
        if (processedEventRepository.existsById(eventId)) {
            logger.warn("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
            meterRegistry.counter("order.event.duplicate", "service", "product-service", "event", eventType)
                    .increment();
            return true;
        }
        return false;
    }

    private void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, OffsetDateTime.now()));
    }
}
