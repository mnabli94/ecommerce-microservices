package com.demo.events.stock;

import com.demo.events.Event;

import java.util.UUID;

public interface StockEvent extends Event {
    UUID orderId();

    default String key() {
        return orderId().toString().split("-")[4];
    }
}
