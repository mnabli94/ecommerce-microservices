package com.demo.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentDTO(
        @NotNull
        @Min(value = 1, message = "quantityToAdd must be at least 1")
        Integer quantityToAdd) {
}
