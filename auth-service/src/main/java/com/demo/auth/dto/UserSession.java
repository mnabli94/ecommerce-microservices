package com.demo.auth.dto;

import java.util.List;

public record UserSession(
        String username,
        List<String> roles,
        List<String> scopes) {
}
