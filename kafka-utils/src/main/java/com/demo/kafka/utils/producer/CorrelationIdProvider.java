package com.demo.kafka.utils.producer;

public interface CorrelationIdProvider {
    String getOrCreate();
}
