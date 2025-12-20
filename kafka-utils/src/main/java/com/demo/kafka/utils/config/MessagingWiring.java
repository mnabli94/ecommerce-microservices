package com.demo.kafka.utils.config;

import com.demo.kafka.utils.EventConsumer;
import com.demo.kafka.utils.KafkaModule;
import com.demo.kafka.utils.producer.CorrelationIdProvider;
import com.demo.kafka.utils.producer.EventPublisher;
import com.demo.kafka.utils.producer.MdcCorrelationIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MessagingWiring {

    @Value("${KAFKA_BOOTSTRAP_SERVERS}")
    private String bootstrapServers;

    @Value("${spring.application.name:unknown-service}")
    private String producerName;

    @Bean
    public KafkaModule kafkaModule(ObjectMapper mapper) {
        var common = Map.<String, Object>of(
                "bootstrap.servers", bootstrapServers,
                "acks", "all"
        );
        var producer = Map.<String, Object>of(
                "delivery.timeout.ms", 30000,
                "request.timeout.ms", 15000,
                "linger.ms", 5
        );
        var consumer = Map.<String, Object>of(
                "auto.offset.reset", "earliest"
        );
        return new KafkaModule(common, producer, consumer, mapper);
    }

    @Bean
    public CorrelationIdProvider correlationIdProvider() {
        return new MdcCorrelationIdProvider();
    }

    @Bean
    public EventPublisher eventPublisher(KafkaModule module, CorrelationIdProvider correlationIdProvider) {
        return module.publisher(producerName, correlationIdProvider);
    }

    @Bean
    public EventConsumer eventConsumer(KafkaModule module) {
        return new EventConsumer(module);
    }
}
