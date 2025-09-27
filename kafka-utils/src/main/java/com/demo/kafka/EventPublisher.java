package com.demo.kafka;

import com.demo.kafka.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafkaTemplate = kafka;
    }

    public <T extends Event> void publish(String topic, T event) {
        try {
            kafkaTemplate.send(topic, event.key(), event);
            logger.info("Successfully published to topic: {}, key: {}, createdAt: {} with event: {}", topic, event.key(), event.createdAt(), event);
        } catch (Exception e) {
            logger.error("Failed to publish to topic: {} key: {}, createdAt: {}, error: {}", topic, event.key(), event.createdAt(), e.getMessage(), e);
        }
    }

}
