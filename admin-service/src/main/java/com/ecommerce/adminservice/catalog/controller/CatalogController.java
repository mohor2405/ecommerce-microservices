package com.ecommerce.adminservice.catalog.controller;

import com.ecommerce.adminservice.catalog.service.CatalogService;
import com.ecommerce.adminservice.category.dto.response.CategoryResponse;
import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.response.CatalogProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public, read-only catalog (GET /api/v1/catalog/** is permitAll in SecurityConfig).
 * No "active" parameter: customers can never see inactive products.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(catalogService.getActiveCategories());
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<CatalogProductResponse>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        ProductFilter filter = new ProductFilter(name, categoryId, minPrice, maxPrice, null);
        return ResponseEntity.ok(catalogService.getProducts(filter, pageable));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getProduct(id));
    }
}
