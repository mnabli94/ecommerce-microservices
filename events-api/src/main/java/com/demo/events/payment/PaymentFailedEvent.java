package com.demo.events.payment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        OffsetDateTime occurredAt
) implements PaymentEvent {
}
