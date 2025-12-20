package com.demo.kafka.utils.producer;

public class MdcCorrelationIdProvider implements CorrelationIdProvider {

    private static final String MDC_KEY = "correlationId";

    @Override
    public String getOrCreate() {
        String id = org.slf4j.MDC.get(MDC_KEY);
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
            org.slf4j.MDC.put(MDC_KEY, id);
        }
        return id;
    }
}

