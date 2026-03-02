package com.demo.events.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        String paymentReference,
        BigDecimal amount,
        OffsetDateTime occurredAt
) implements PaymentEvent {
}
