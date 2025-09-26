package com.demo.order.messaging.events;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]")
        OffsetDateTime createdAt,
        List<OrderCreatedEvent.Item> items) implements Event {

    public String key() {return orderId.toString().split("-")[4];}
}
