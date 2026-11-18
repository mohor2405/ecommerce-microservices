package com.ecommerce.adminservice.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Full product view for ADMIN endpoints (includes audit fields). */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        boolean active,
        CategorySummary category,
        Long createdBy,
        Long updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
