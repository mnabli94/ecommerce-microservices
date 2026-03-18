package com.demo.payment.mapper;

import com.demo.payment.dto.PaymentDTO;
import com.demo.payment.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentDTO toDto(Payment entity) {
        if (entity == null) return null;
        return new PaymentDTO(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getPaymentReference(),
                entity.getFailureReason(),
                entity.getCreatedAt()
        );
    }
}
