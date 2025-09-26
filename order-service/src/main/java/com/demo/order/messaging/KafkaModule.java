package com.demo.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KafkaModule {

    private final ObjectMapper objectMapper;
    private final Map<String, Object> producerProps = new HashMap<>();
    private final Map<String, Object> consumerProps = new HashMap<>();
    private final ProducerFactory<String, Object> producerFactory;
    private final ConsumerFactory<String, Object> consumerFactory;
    private final ConcurrentKafkaListenerContainerFactory<String, Object> containerFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, ConcurrentMessageListenerContainer<String, ?>> containers = new ConcurrentHashMap<>();

    public KafkaModule(Map<String, Object> common, Map<String, Object> producer, Map<String, Object> consumer, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initProducerProperties(common, producer);
        initConsumerProperties(common, consumer);

        producerFactory = new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new JsonSerializer<>(this.objectMapper));
        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(Object.class, this.objectMapper, false));
        containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(consumerFactory);
    }

    private void initConsumerProperties(Map<String, Object> common, Map<String, Object> consumer) {
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, common.get("bootstrap.servers"));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.putAll(consumer == null ? Map.of() : consumer);
    }

    private void initProducerProperties(Map<String, Object> common, Map<String, Object> producer) {
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, common.get("bootstrap.servers"));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // pas de __TypeId__
        producerProps.put(ProducerConfig.ACKS_CONFIG, common.getOrDefault("acks", "all"));
        producerProps.putAll(producer == null ? Map.of() : producer);
    }

    public EventPublisher publisher() {
        return new EventPublisher(kafkaTemplate);
    }

    public <T> ConcurrentMessageListenerContainer<String, T> registerConsumer(String topic, String groupId, Class<T> valueType, Consumer<T> handler) {
        var props = new HashMap<>(consumerProps);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());

        var consumerFactory = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(valueType, objectMapper, false));
        var container = new ConcurrentMessageListenerContainer<>(consumerFactory, new ContainerProperties(topic));

        container.setupMessageListener((MessageListener<String, T>) record -> handler.accept(record.value()));
        container.start();
        containers.put(topic + "-" + groupId, container);
        return container;
    }

    @PreDestroy
    private void shutdown(){
        containers.values().forEach(ConcurrentMessageListenerContainer::stop);
    }
}
