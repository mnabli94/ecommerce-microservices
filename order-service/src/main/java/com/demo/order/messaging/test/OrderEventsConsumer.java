package com.demo.order.messaging.test;

import com.demo.order.messaging.KafkaModule;
import com.demo.order.messaging.Topics;
import com.demo.order.messaging.events.OrderConfirmedEvent;
import com.demo.order.messaging.events.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderEventsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventsConsumer.class);
    private final MeterRegistry meterRegistry;
    private final KafkaModule module;

    public OrderEventsConsumer(MeterRegistry meterRegistry, KafkaModule module) {
        this.meterRegistry = meterRegistry;
        this.module = module;
    }

    @PostConstruct
    void init() {
        module.registerConsumer(Topics.ORDER_CREATED, "order-service-test", OrderCreatedEvent.class, this::onOrderCreated);
        module.registerConsumer(Topics.ORDER_CONFIRMED, "order-service-test", OrderConfirmedEvent.class,
                event -> logger.info("Received OrderConfirmed: key={}, total={}, createdAt={}", event.key(), event.totalAmount(), event.createdAt()));
    }

    public void onOrderCreated(OrderCreatedEvent event) {
        meterRegistry.counter("order.event.consumed", "service", "order-service", "event", "order-created").increment();
        logger.info("Received OrderCreated: key={}, total={}, createdAt={}", event.key(), event.totalAmount(), event.createdAt());
    }

}
