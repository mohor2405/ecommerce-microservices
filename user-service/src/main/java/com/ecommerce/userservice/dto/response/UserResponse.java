package com.ecommerce.userservice.dto.response;

import com.ecommerce.userservice.entity.Role;

import java.time.Instant;

/** Public view of a user. Notice: there is NO password field, so it can never leak. */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
}
