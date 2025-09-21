package com.demo.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry registry) {
        Counter.builder("order.created")
                .tag("service", "order-service")
                .description("Number of orders created")
                .register(registry);
        Counter.builder("order.confirmed")
                .tag("service", "order-service")
                .description("Number of orders confirmed")
                .register(registry);
        Counter.builder("order.cancelled")
                .tag("service", "order-service")
                .description("Number of orders cancelled")
                .register(registry);
    }
}