package com.demo.events.stock;

import com.demo.events.order.Item;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        OffsetDateTime occurredAt
) implements StockEvent {
}
