package com.ecommerce.adminservice.exception;

/**
 * The request breaks a business rule that the client can fix
 * (e.g. insufficient stock, minPrice greater than maxPrice). Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
