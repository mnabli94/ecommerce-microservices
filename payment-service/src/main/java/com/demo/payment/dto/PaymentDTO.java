package com.demo.payment.dto;

import com.demo.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentDTO(
        UUID id,
        UUID orderId,
        String userId,
        BigDecimal amount,
        PaymentStatus status,
        String paymentReference,
        String failureReason,
        OffsetDateTime createdAt
) {}
