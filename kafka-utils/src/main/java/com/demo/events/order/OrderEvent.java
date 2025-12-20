package com.demo.events.order;

import com.demo.events.Event;

import java.util.UUID;

public interface OrderEvent extends Event {
     UUID orderId();
    default String key() {return orderId().toString().split("-")[4];}
}
