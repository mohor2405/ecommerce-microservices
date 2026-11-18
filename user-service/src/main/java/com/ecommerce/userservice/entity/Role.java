package com.ecommerce.userservice.entity;

/**
 * Roles used for authorization. Stored as a String column (EnumType.STRING) so that
 * re-ordering the constants never corrupts existing data.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
