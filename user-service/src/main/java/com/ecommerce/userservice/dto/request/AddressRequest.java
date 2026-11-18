package com.ecommerce.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating an address.
 * defaultAddress is optional: null/false keeps the current default, true makes this the default.
 */
public record AddressRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 must be at most 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must be at most 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[A-Za-z0-9 -]{3,10}$",
                message = "Postal code must be 3 to 10 letters, digits, spaces or hyphens")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        Boolean defaultAddress
) {
}
