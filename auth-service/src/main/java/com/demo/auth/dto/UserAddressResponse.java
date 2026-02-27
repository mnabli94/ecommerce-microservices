package com.demo.auth.dto;

import com.demo.auth.entity.UserAddressLabel;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAddressResponse(
        UUID id,
        UserAddressLabel label,
        String firstName,
        String lastName,
        String phoneNumber,
        String street,
        String city,
        String postalCode,
        String country,
        Instant createdAt,
        Instant updatedAt,
        boolean defaultAddress) {
}
