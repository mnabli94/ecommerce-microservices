package com.demo.order.dto.in;

import com.demo.order.dto.ShippingAddressDTO;
import com.demo.order.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderInDTO(
        @NotNull(message = "Status cannot be null")
        OrderStatus status,
        @Valid
        ShippingAddressDTO shippingAddress,
        @NotNull(message = "Items cannot be null")
        @Size(min = 1, message = "Order must have at least one item")
        @Valid
        List<OrderItemInDTO> orderItems,
        BigDecimal totalAmount
) {}
