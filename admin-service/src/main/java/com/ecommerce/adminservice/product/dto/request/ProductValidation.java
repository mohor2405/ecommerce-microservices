package com.ecommerce.adminservice.product.dto.request;

/** Shared validation patterns/messages for product requests (DRY). */
public final class ProductValidation {

    public static final String SKU_PATTERN = "^[A-Za-z0-9-]{3,50}$";
    public static final String SKU_MESSAGE = "SKU must be 3 to 50 letters, digits or hyphens";

    public static final String IMAGE_URL_PATTERN = "^https?://\\S+$";
    public static final String IMAGE_URL_MESSAGE = "Image URL must start with http:// or https://";

    private ProductValidation() {
    }
}
