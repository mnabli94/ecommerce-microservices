package com.demo.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductDTO(Long id,
                         @NotBlank(message = "name cannot be null or emtpy") @Size(min = 2, message = "minimal name size is 2") String name,
                         @NotNull @DecimalMin(value = "0.01", message = "Cannot accept an amount less than 0.01") BigDecimal price,
                         boolean inStock,
                         @NotNull Long categoryId) {
}
