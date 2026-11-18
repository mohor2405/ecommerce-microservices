package com.ecommerce.adminservice.product.specification;

import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a dynamic WHERE clause from optional filters (Specification pattern).
 *
 * <p>Each method returns one small condition; {@link #from(ProductFilter)} combines only the
 * filters that were actually provided with AND. This avoids writing a separate repository
 * method for every possible filter combination.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Combines all non-null filters with AND. No filters = all products. */
    public static Specification<Product> from(ProductFilter filter) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (filter.name() != null && !filter.name().isBlank()) {
            specs.add(nameContains(filter.name()));
        }
        if (filter.categoryId() != null) {
            specs.add(inCategory(filter.categoryId()));
        }
        if (filter.minPrice() != null) {
            specs.add(priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            specs.add(priceAtMost(filter.maxPrice()));
        }
        if (filter.active() != null) {
            specs.add(hasActive(filter.active()));
        }
        return Specification.allOf(specs);
    }

    /** WHERE LOWER(name) LIKE '%term%' (wildcard characters typed by the user are escaped). */
    public static Specification<Product> nameContains(String term) {
        String escaped = term.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> hasActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    /** Used by the public catalog: hide products whose category is deactivated. */
    public static Specification<Product> inActiveCategory() {
        return (root, query, cb) -> cb.isTrue(root.get("category").get("active"));
    }
}
