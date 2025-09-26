package com.demo.order.messaging;

import com.demo.order.messaging.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, Object> kafka;

    public EventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public <T extends Event> void publish(String topic, T event) {
        kafka.send(topic, event.key(), event)
                .whenComplete((res, exp) -> {
                    logger.info("{} event with key={} and created at={} was sent", topic, event.key(), event.createdAt());
                });
    }

}
