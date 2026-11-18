package com.ecommerce.userservice.exception;

/** Thrown when creating something that must be unique (e.g. email) and already exists. HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
