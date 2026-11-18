package com.ecommerce.adminservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code app.jwt.*} from application.yml. The app refuses to start if a value is missing.
 *
 * <p>Unlike the User Service there is no expiration setting: this service never ISSUES tokens,
 * it only verifies tokens issued by the User Service.
 *
 * @param secret Base64-encoded HMAC key, identical to the User Service's secret
 * @param issuer expected "iss" claim of incoming tokens ("user-service")
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer
) {
}
