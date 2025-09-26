package com.demo.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MessagingWiring {

    @Bean
    public KafkaModule kafkaModule(ObjectMapper mapper) {
        var common = Map.<String, Object>of(
                "bootstrap.servers", "localhost:9092",
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
}
