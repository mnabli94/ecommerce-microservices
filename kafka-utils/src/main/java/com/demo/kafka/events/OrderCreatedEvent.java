package com.demo.kafka.events;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String status,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]")
        OffsetDateTime createdAt,
        List<Item> items
) implements Event {
    public record Item(Long productId, int quantity, BigDecimal unitPrice) {}
    public String key() { return orderId.toString().split("-")[4]; }
}