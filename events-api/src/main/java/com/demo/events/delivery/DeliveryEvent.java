package com.demo.events.delivery;

import com.demo.events.Event;

import java.util.UUID;

public interface DeliveryEvent extends Event {
    UUID orderId();

    default String key() {
        return orderId().toString().split("-")[4];
    }
}
