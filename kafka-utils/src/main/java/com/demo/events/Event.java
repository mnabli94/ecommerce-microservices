package com.demo.events;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface Event {
    String key();
    OffsetDateTime occurredAt();
}
