package com.demo.product.messaging;

import com.demo.kafka.EventConsumer;
import com.demo.kafka.EventPublisher;
import com.demo.kafka.KafkaModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MessagingWiring {

    @Value("${KAFKA_BOOTSTRAP_SERVERS}")
    private String bootstrapServers;

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
    public EventPublisher eventPublisher(KafkaModule module) {
        return module.publisher();
    }

    @Bean
    public EventConsumer eventConsumer(KafkaModule module) {
        return new EventConsumer(module);
    }
}
