package com.demo.order.messaging.events;

import java.time.OffsetDateTime;

public interface Event {
    default String key() { return null; }
    OffsetDateTime createdAt();
}
