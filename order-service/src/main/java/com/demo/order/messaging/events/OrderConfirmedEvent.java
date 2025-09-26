package com.demo.order.messaging.events;

import java.util.UUID;

public record OrderConfirmedEvent(UUID orderId, Long productId, Integer quantity) {
    //TODO
}
