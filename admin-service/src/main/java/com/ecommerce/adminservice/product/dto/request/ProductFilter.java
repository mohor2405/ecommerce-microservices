package com.ecommerce.adminservice.product.dto.request;

import java.math.BigDecimal;

/**
 * Optional filters for product listings. Every field may be null, meaning "don't filter on this".
 *
 * @param name       case-insensitive "contains" match on product name
 * @param categoryId exact category
 * @param minPrice   price &gt;= minPrice
 * @param maxPrice   price &lt;= maxPrice
 * @param active     admin listing only: true/false, or null for both
 */
public record ProductFilter(
        String name,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean active
) {
    /** True when both bounds are given and min &gt; max (e.g. minPrice=500&amp;maxPrice=100). */
    public boolean hasInvalidPriceRange() {
        return minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0;
    }
}
