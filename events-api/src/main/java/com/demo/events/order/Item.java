package com.demo.events.order;

import java.math.BigDecimal;

public record Item(
        Long productId,
        int quantity,
        BigDecimal unitPrice
) {
}
