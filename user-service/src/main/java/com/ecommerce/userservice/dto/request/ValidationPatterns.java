package com.ecommerce.userservice.dto.request;

/**
 * Shared regular expressions so the same rule is not duplicated across DTOs (DRY).
 */
public final class ValidationPatterns {

    /** At least one lowercase letter, one uppercase letter and one digit. */
    public static final String STRONG_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$";
    public static final String STRONG_PASSWORD_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter and one digit";

    /** 10 to 15 digits, optionally prefixed with '+'. Null values are allowed (phone is optional). */
    public static final String PHONE = "^\\+?[0-9]{10,15}$";
    public static final String PHONE_MESSAGE = "Phone must contain 10 to 15 digits and may start with +";

    private ValidationPatterns() {
    }
}
