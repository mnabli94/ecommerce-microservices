package com.demo.events.order;

import com.demo.events.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        OffsetDateTime occurredAt
) implements OrderEvent {

}
