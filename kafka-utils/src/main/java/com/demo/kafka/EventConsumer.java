package com.demo.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.function.Consumer;

public class EventConsumer<T> {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    private final KafkaModule kafkaModule;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventConsumer(KafkaModule kafkaModule, KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaModule = kafkaModule;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ConcurrentMessageListenerContainer<String, T> register(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        return kafkaModule.registerConsumer(topic, groupId, valueType, handler);
    }

    public ConcurrentMessageListenerContainer<String, T> registerWithDlq(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        return registerWithDlq(topic, groupId, topic + ".dlq", valueType, handler);
    }

    public ConcurrentMessageListenerContainer<String, T> registerWithDlq(String topic, String groupId, String dlqTopic, Class<T> valueType, Consumer<T> handler) {
        return kafkaModule.registerConsumer(topic, groupId, valueType, event -> {
            try {
                handler.accept(event);
                logger.info("Successfully processed event from topic: {}", topic);
            } catch (Exception e) {
                logger.error("Failed to process event from topic: {}, sending to DLQ: {}, error: {}", topic, event, e.getMessage(), e);
                kafkaTemplate.send(dlqTopic, event);
                throw e;
            }
        });
    }

    public ConcurrentMessageListenerContainer<String, T> registerDlqConsumer(String dlqTopic, String dlqGroupId, Class<T> valueType, Consumer<T> handler) {
        return kafkaModule.registerConsumer(dlqTopic, dlqGroupId, valueType, event -> {
            logger.warn("Processing DLQ event from topic: {}, event: {}", dlqTopic, event);
            handler.accept(event);
        });
    }

}
