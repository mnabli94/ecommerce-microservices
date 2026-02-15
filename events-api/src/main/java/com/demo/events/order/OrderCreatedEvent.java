package com.demo.events.order;

import com.demo.events.Event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        BigDecimal totalAmount,
        OffsetDateTime occurredAt,
        List<Item> items
) implements OrderEvent {
    public record Item(
            Long productId,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}
