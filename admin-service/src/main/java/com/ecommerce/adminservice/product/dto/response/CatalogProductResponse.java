package com.ecommerce.adminservice.product.dto.response;

import java.math.BigDecimal;

/**
 * Public product view for the customer catalog. It intentionally leaves out internal data:
 * no createdBy/updatedBy (admin ids), no active flag (only active products are shown), and
 * no exact stock count, just whether it can be bought.
 */
public record CatalogProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        boolean inStock,
        String imageUrl,
        CategorySummary category
) {
}
