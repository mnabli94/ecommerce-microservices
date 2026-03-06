package com.demo.events.order;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCancellationRequestedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        OffsetDateTime occurredAt
) implements OrderEvent {
}
