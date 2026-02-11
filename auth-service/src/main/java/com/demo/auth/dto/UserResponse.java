package com.demo.auth.dto;

import java.time.LocalDateTime;

public record UserResponse(
        String username,
        String roles,
        LocalDateTime createAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {
}