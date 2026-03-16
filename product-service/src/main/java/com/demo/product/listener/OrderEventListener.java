package com.demo.product.listener;

import com.demo.events.order.OrderCancelledEvent;
import com.demo.events.order.OrderConfirmedEvent;
import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderTopics;
import com.demo.kafka.utils.entity.ProcessedEvent;
import com.demo.kafka.utils.repository.ProcessedEventRepository;
import com.demo.product.service.StockService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
public class OrderEventListener {

    private final ProcessedEventRepository processedEventRepository;
    private final StockService stockService;
    private final MeterRegistry meterRegistry;

    public OrderEventListener(ProcessedEventRepository processedEventRepository,
                              StockService stockService,
                              MeterRegistry meterRegistry) {
        this.processedEventRepository = processedEventRepository;
        this.stockService = stockService;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = OrderTopics.ORDER_CREATED, groupId = "product-service")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        log.info("Received OrderCreated: orderId={}, items={}", event.orderId(), event.items().size());
        stockService.reserveStock(event);
        count("order-created");
    }

    @KafkaListener(topics = OrderTopics.ORDER_CREATED + ".dlq", groupId = "product-service-dlq")
    public void onOrderCreatedDlq(OrderCreatedEvent event) {
        log.error("DLQ: order.created.dlq event={}", event);
    }

    @Deprecated // TODO to be placed on delivery-service
    @KafkaListener(topics = OrderTopics.ORDER_CONFIRMED, groupId = "product-service")
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        log.info("Received OrderConfirmed: orderId={}", event.orderId());
        count("order-confirmed");
    }

    @Deprecated // TODO to be placed on delivery-service
    @KafkaListener(topics = OrderTopics.ORDER_CONFIRMED + ".dlq", groupId = "product-service-dlq")
    public void onOrderConfirmedDlq(OrderConfirmedEvent event) {
        log.error("DLQ: order.confirmed.dlq event={}", event);
    }

    @KafkaListener(topics = OrderTopics.ORDER_CANCELLED, groupId = "product-service")
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (isDuplicate(event.eventId(), event.getClass().getSimpleName())) {
            return;
        }
        markProcessed(event.eventId(), event.getClass().getSimpleName());
        log.info("Received OrderCancelled: orderId={}, reason={}", event.orderId(), event.reason());
        stockService.releaseStock(event.orderId());
        count("order-cancelled");
    }

    @KafkaListener(topics = OrderTopics.ORDER_CANCELLED + ".dlq", groupId = "product-service-dlq")
    public void onOrderCancelledDlq(OrderCancelledEvent event) {
        log.error("DLQ: order.cancelled.dlq event={}", event);
    }

    private boolean isDuplicate(UUID eventId, String eventType) {
        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
            meterRegistry.counter("order.event.duplicate", "service", "product-service", "event", eventType)
                    .increment();
            return true;
        }
        return false;
    }

    private void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, OffsetDateTime.now()));
    }

    private void count(String eventName) {
        meterRegistry.counter("order.event.consumed", "service", "product-service", "event", eventName).increment();
    }
}
