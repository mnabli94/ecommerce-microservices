package com.demo.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentWebhookDTO(
        @NotNull UUID orderId,
        @NotBlank String status,
        String reason
) {}
