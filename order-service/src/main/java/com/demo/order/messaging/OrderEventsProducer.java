package com.demo.order.messaging;

import com.demo.order.messaging.events.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventsProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> orderCreatedEventKafkaTemplate;
    private static final Logger logger = LoggerFactory.getLogger(OrderEventsProducer.class);
    private final MeterRegistry meterRegistry;

    public OrderEventsProducer(KafkaTemplate<String, OrderCreatedEvent> orderCreatedEventKafkaTemplate,
                               MeterRegistry meterRegistry) {
        this.orderCreatedEventKafkaTemplate = orderCreatedEventKafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void publish(OrderCreatedEvent evt) {
        String topic = "order.created";

        String key = evt.orderId().toString();
        orderCreatedEventKafkaTemplate.send(topic, key, evt)
                .whenComplete((res, exp) -> {
                    logger.info("order.created event with key={} and created at={} was sent", key, evt.createdAt());
                    meterRegistry.counter("order.event.published", "service", "order-service", "event", "order-created").increment();
                });
    }

}
