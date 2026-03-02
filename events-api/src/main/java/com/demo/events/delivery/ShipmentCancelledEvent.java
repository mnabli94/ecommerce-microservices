package com.demo.events.delivery;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentCancelledEvent(
        UUID eventId,
        UUID orderId,
        OffsetDateTime occurredAt
) implements DeliveryEvent {
}
