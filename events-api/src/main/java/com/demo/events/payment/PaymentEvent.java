package com.demo.events.payment;

import com.demo.events.Event;

import java.util.UUID;

public interface PaymentEvent extends Event {
    UUID orderId();

    default String key() {
        return orderId().toString().split("-")[4];
    }
}
