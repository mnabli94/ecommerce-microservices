package com.demo.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface Event {
    UUID eventId();
    String key();
    OffsetDateTime occurredAt();
}
