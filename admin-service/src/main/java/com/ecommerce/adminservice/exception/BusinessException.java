package com.ecommerce.adminservice.exception;

/**
 * The request is valid, but it conflicts with the current state of the data
 * (e.g. deleting a category that still has products). Mapped to HTTP 409.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
