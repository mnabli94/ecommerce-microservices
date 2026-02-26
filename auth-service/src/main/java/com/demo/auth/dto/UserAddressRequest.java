package com.demo.auth.dto;

import com.demo.auth.entity.UserAddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAddressRequest(

        @Size(max = 50)
        UserAddressLabel label,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phoneNumber,

        @NotBlank(message = "Street is required")
        @Size(max = 255)
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 20)
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code")
        String country) {
}
