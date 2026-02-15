package com.demo.kafka.utils.producer;

import com.demo.events.Event;
import com.demo.kafka.utils.headers.KafkaHeadersConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

public class EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String producerName;
    private final CorrelationIdProvider correlationIdProvider;

    public EventPublisher(KafkaTemplate<String, Object> kafka, String producerName, CorrelationIdProvider correlationIdProvider) {
        this.kafkaTemplate = kafka;
        this.producerName = producerName;
        this.correlationIdProvider = correlationIdProvider;
    }

    public <T extends Event> void publish(String topic, T event) {
        try {
            var correlationId = correlationIdProvider.getOrCreate();
            var message = MessageBuilder.withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, event.key())
                    .setHeader(KafkaHeadersConst.EVENT_ID, event.eventId().toString())
                    .setHeader(KafkaHeadersConst.EVENT_TYPE, event.getClass().getSimpleName())
                    .setHeader(KafkaHeadersConst.OCCURRED_AT, event.occurredAt())
                    .setHeader(KafkaHeadersConst.PRODUCER, producerName)
                    .setHeader(KafkaHeadersConst.CORRELATION_ID, correlationId)
                    .build();
            kafkaTemplate.send(message);
            logger.info("Successfully published to topic: {}, key: {}, occurredAt: {} with event: {}", topic, event.key(), event.occurredAt(), event);
        } catch (Exception e) {
            logger.error("Failed to publish to topic: {} key: {}, occurredAt: {}, error: {}", topic, event.key(), event.occurredAt(), e.getMessage(), e);
        }
    }

}
