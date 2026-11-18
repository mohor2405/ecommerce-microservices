package com.ecommerce.adminservice.catalog.service;

import com.ecommerce.adminservice.category.dto.response.CategoryResponse;
import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.response.CatalogProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Read-only public catalog for customers. Only ACTIVE products in ACTIVE categories are visible.
 * Kept separate from the admin services so public rules can never leak admin-only data.
 */
public interface CatalogService {

    List<CategoryResponse> getActiveCategories();

    PageResponse<CatalogProductResponse> getProducts(ProductFilter filter, Pageable pageable);

    CatalogProductResponse getProduct(Long id);
}
