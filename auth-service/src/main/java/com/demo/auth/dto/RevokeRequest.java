package com.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeRequest(@NotBlank(message = "refresh token cannot be null or empty") String refreshToken) {
}

