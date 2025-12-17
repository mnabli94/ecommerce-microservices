package com.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank  @Size(min = 2, message = "minimal name size is 2")
                                String username,
                                @NotBlank(message = "password cannot be null or empty")
                                @Size(min = 6, message = "minimal name size is 6")
                                String password,
                                @NotBlank
                                String roles) {

}