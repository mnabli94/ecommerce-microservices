package com.demo.events.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        OffsetDateTime occurredAt
) implements PaymentEvent {
}
