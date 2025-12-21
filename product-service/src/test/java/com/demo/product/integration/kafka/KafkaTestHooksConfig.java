package com.demo.product.integration.kafka;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;

@TestConfiguration
public class KafkaTestHooksConfig {

    @Bean
    public CountDownLatch orderCreatedLatch() {
        return new CountDownLatch(1);
    }
}