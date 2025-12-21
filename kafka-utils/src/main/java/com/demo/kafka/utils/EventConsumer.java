package com.demo.kafka.utils;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import java.util.function.Consumer;

public class EventConsumer<T> {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    private final KafkaModule kafkaModule;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventConsumer(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
        this.kafkaTemplate = kafkaModule.kafkaTemplate();
    }

    public ConcurrentMessageListenerContainer<String, T> register(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        return kafkaModule.registerConsumer(topic, groupId, valueType, handler);
    }

    public void registerWithDlq(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        registerWithDlq(topic, groupId, topic + ".dlq", valueType, handler);
    }

    public void registerWithDlq(String topic, String groupId, String dlqTopic, Class<T> valueType, Consumer<T> handler) {
        kafkaModule.registerConsumer(topic, groupId, valueType, event -> {
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

    public void registerDlqConsumer(String dlqTopic, String dlqGroupId, Class<T> valueType, Consumer<T> handler) {
        kafkaModule.registerConsumer(dlqTopic, dlqGroupId, valueType, event -> {
            logger.warn("Processing DLQ event from topic: {}, event: {}", dlqTopic, event);
            handler.accept(event);
        });
    }

    public ConcurrentMessageListenerContainer<String, T> registerRecord(String topic, String groupId, Class<T> valueType, Consumer<ConsumerRecord<String, T>> handler) {
        return kafkaModule.registerConsumerRecord(topic, groupId, valueType, handler);
    }

    public ConcurrentMessageListenerContainer<String, T> registerWithDlqRecord(String topic, String groupId, String dlqTopic, Class<T> valueType, Consumer<ConsumerRecord<String, T>> handler) {
        return registerRecord(topic, groupId, valueType, record -> {
            try {
                handler.accept(record);
            } catch (Exception e) {
                logger.error("Failed topic={}, sending to dlq={}, key={}, error={}", topic, dlqTopic, record.key(), e.getMessage(), e);
                kafkaTemplate.send(dlqTopic, record.key(), record.value()); // garde au moins la key
                throw e;
            }
        });
    }

}
