package com.demo.product.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductDTO(Long id,
                         @NotBlank String name,
                         @NotNull @DecimalMin(value = "0.01", message = "Cannot accept an amount less than 0.01") BigDecimal price,
                         boolean inStock) {
}
