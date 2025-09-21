package com.demo.order.dto.in;

import com.demo.order.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderInDTO(
        @NotNull(message = "Status cannot be null")
        OrderStatus status,
        @Size(max = 255, message = "Shipping address must be at most 255 characters")
        String shippingAddress,
        @NotNull(message = "Items cannot be null")
        @Size(min = 1, message = "Order must have at least one item")
        @Valid
        List<OrderItemInDTO> orderItems,
        BigDecimal totalAmount
) {}
