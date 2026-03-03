package com.demo.order.dto.out;

import com.demo.order.dto.ShippingAddressDTO;
import com.demo.order.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderOutDTO(
        UUID id,
        String userId,
        String contactEmail,
        @NotNull(message = "Status cannot be null")
        OrderStatus status,
        @Valid
        ShippingAddressDTO shippingAddress,
        @NotNull(message = "Items cannot be null")
        @Size(min = 1, message = "Order must have at least one item")
        @Valid
        List<OrderItemOutDTO> orderItems,
        BigDecimal totalAmount,
        String paymentReference,
        String cancellationReason,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]")
        OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]")
        OffsetDateTime confirmedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]")
        OffsetDateTime shippedAt
) {}
