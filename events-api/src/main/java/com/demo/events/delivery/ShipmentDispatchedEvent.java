package com.demo.events.delivery;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentDispatchedEvent(
        UUID eventId,
        UUID orderId,
        String trackingNumber,
        OffsetDateTime occurredAt
) implements DeliveryEvent {
}
