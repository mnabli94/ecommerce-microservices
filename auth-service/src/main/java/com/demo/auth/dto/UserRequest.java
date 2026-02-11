package com.demo.auth.dto;

import com.demo.auth.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserRequest(
    @NotBlank(groups = Create.class, message = "Username is required")
    @Size(min = 2, groups = Create.class, message = "Username must be at least 2 characters")
    String username,

    @NotBlank(groups = Create.class, message = "Password is required for new users")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @NotEmpty(groups = Create.class, message = "At least one role is required")
    Set<UserRole> roles
) {
    public interface Create {}
    public interface Update {}
}