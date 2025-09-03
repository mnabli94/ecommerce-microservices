package com.demo.order.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemOutDTO(
        UUID id,
//        @NotNull(message = "Product cannot be null")
        @JsonProperty(value = "productDetails")
        ProductDTO product,
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        @NotNull(message = "Unit price cannot be null")
        @PositiveOrZero(message = "Unit price must be positive or zero")
        BigDecimal unitPrice
) {}
