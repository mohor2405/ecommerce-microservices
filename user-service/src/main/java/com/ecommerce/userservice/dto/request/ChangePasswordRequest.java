package com.ecommerce.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for PUT /api/v1/users/me/password. */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        String newPassword
) {
}
