package com.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "username cannot be null or empty") String username,
                           @NotBlank(message = "password cannot be null or empty") String password) {
}

