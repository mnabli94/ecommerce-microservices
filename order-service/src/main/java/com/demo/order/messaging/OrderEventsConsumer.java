package com.demo.order.messaging;

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
        module.registerConsumer(
                Topics.ORDER_CREATED,
                "order-service-test",
                OrderCreatedEvent.class,
                event -> logger.info("Received OrderCreated: key={}, total={}, createdAt={}", event.key(), event.totalAmount(), event.createdAt())
        );
    }
/*
    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "order-service-test")
    public void onOrderCreated(OrderCreatedEvent event) {
        meterRegistry.counter("order.event.consumed", "service", "order-service", "event", "order-created").increment();
        logger.info("Received OrderCreated: id={}, total={}, createdAt={}", event.orderId(), event.totalAmount(), event.createdAt());
    }
*/
}
