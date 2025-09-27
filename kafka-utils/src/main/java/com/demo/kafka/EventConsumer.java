package com.demo.kafka;

import com.demo.kafka.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.function.Consumer;

public class EventConsumer {
    private final KafkaModule kafkaModule;

    public EventConsumer(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public <T> ConcurrentMessageListenerContainer<String, T> register(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        return kafkaModule.registerConsumer(topic, groupId, valueType, handler);
    }

}
