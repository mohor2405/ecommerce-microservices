package com.ecommerce.userservice.exception;

/** Thrown when a password-change request is invalid (wrong current password, same password). HTTP 400. */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
