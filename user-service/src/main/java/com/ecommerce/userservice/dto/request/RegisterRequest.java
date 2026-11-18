package com.ecommerce.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/auth/register.
 * A Java record is an immutable data carrier, which is a good fit for DTOs.
 * The validation annotations are checked when the controller parameter is marked with @Valid.
 */
public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        String password,

        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone
) {
}
