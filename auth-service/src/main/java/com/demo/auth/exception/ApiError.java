package com.demo.auth.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        String error,
        String message,
        Instant timestamp,
        Map<String, List<String>> details) {

    public ApiError(String error, String message, Map<String, List<String>> details) {
        this(error, message, Instant.now(), details);
    }

    public ApiError(String error, String message) {
        this(error, message, Instant.now(), null);
    }
}
