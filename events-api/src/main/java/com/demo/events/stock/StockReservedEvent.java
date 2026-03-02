package com.demo.events.stock;

import com.demo.events.order.Item;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        String contactEmail,
        String paymentMethodId,
        BigDecimal totalAmount,
        List<Item> items,
        OffsetDateTime occurredAt
) implements StockEvent {
}
