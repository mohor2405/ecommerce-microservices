package com.ecommerce.adminservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Same error JSON format as the User Service, so clients see one consistent shape across services.
 * Used by GlobalExceptionHandler and RestSecurityErrorHandler.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path,
                                   Map<String, String> validationErrors) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, path, validationErrors);
    }
}
