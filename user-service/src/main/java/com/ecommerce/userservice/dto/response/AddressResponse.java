package com.ecommerce.userservice.dto.response;

import java.time.Instant;

public record AddressResponse(
        Long id,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt
) {
}
