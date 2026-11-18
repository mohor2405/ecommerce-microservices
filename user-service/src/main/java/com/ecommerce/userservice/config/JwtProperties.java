package com.ecommerce.userservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding of the {@code app.jwt.*} properties from application.yml.
 * {@code @Validated} makes the application fail fast at startup if they are missing.
 *
 * @param secret       Base64-encoded HMAC key (at least 256 bits)
 * @param expirationMs token lifetime in milliseconds
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long expirationMs
) {
}
