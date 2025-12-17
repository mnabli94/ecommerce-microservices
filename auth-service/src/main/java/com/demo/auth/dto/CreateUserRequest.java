package com.demo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank @Size(min = 2, message = "minimal username size is {min}")
                                String username,
                                @NotBlank @Size(min = 6, message = "minimal password size is {min}")
                                String password,
                                @NotBlank
                                String roles) {

}