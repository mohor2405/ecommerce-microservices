package com.ecommerce.adminservice.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for PUT /api/v1/admin/products/{id}.
 *
 * <p>There is deliberately NO stockQuantity here: stock only changes through
 * PATCH /products/{id}/stock, so an ordinary edit can never overwrite a stock change.
 *
 * @param active optional: send true to re-activate a soft-deleted product; null = unchanged
 */
public record UpdateProductRequest(

        @NotBlank(message = "SKU is required")
        @Pattern(regexp = ProductValidation.SKU_PATTERN, message = ProductValidation.SKU_MESSAGE)
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Category id is required")
        Long categoryId,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        @Pattern(regexp = ProductValidation.IMAGE_URL_PATTERN, message = ProductValidation.IMAGE_URL_MESSAGE)
        String imageUrl,

        Boolean active
) {
}
