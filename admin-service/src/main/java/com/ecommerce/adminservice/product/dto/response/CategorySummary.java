package com.ecommerce.adminservice.product.dto.response;

/** Small nested view of a product's category: { "id": 1, "name": "Electronics" }. */
public record CategorySummary(Long id, String name) {
}
