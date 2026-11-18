package com.ecommerce.adminservice.exception;

/** A unique value (category name, product SKU) already exists. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
