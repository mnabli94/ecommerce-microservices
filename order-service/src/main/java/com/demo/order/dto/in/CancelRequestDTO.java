package com.demo.order.dto.in;

import jakarta.validation.constraints.NotBlank;

public record CancelRequestDTO(
        @NotBlank(message = "Cancellation reason cannot be blank")
        String reason
) {
}
