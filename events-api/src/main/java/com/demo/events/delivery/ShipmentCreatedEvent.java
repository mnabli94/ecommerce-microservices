package com.demo.events.delivery;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentCreatedEvent(
        UUID eventId,
        UUID orderId,
        String trackingNumber,
        String carrier,
        LocalDate estimatedDelivery,
        OffsetDateTime occurredAt
) implements DeliveryEvent {
}