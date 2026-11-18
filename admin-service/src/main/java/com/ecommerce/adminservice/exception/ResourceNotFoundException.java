package com.ecommerce.adminservice.exception;

/** A requested category/product does not exist (or is not visible). Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
