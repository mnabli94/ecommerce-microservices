package com.demo.events.order;

import com.demo.events.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        UUID orderId,
        String paymentReference,
        OffsetDateTime occurredAt
) implements OrderEvent {
}
