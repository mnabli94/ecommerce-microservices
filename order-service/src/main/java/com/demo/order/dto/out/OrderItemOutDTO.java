package com.demo.order.dto.out;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemOutDTO(
        UUID id,
        @NotBlank(message = "Product ID cannot be empty")
        String productId,
        @NotBlank(message = "Product name cannot be empty")
        String productName,
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        @NotNull(message = "Unit price cannot be null")
        @PositiveOrZero(message = "Unit price must be positive or zero")
        BigDecimal unitPrice
) {}
