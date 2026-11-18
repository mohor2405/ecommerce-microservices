package com.ecommerce.userservice.dto.response;

/** Returned after successful registration or login. */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,      // seconds until the token expires
        UserResponse user
) {
    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
