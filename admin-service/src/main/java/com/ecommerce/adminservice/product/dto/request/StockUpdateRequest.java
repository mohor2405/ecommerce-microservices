package com.ecommerce.adminservice.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/v1/admin/products/{id}/stock.
 * Positive = add stock (delivery arrived), negative = remove stock (damaged items...).
 * Zero is rejected by the service.
 */
public record StockUpdateRequest(

        @NotNull(message = "Quantity change is required")
        @Min(value = -100_000, message = "Quantity change must be at least -100000")
        @Max(value = 100_000, message = "Quantity change must be at most 100000")
        Integer quantityChange
) {
}
