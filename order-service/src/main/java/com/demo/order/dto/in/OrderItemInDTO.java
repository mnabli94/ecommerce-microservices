package com.demo.order.dto.in;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemInDTO(
        @NotBlank(message = "Product ID cannot be empty")
        @Size(max = 50, message = "Product ID must be at most 50 characters")
        String productId,
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        @NotNull(message = "Unit price cannot be null")
        @PositiveOrZero(message = "Unit price must be positive or zero")
        BigDecimal unitPrice
) {}
