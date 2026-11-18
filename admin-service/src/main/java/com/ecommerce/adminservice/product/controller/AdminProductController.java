package com.ecommerce.adminservice.product.controller;

import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.product.dto.request.CreateProductRequest;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.request.StockUpdateRequest;
import com.ecommerce.adminservice.product.dto.request.UpdateProductRequest;
import com.ecommerce.adminservice.product.dto.response.ProductResponse;
import com.ecommerce.adminservice.product.service.ProductService;
import com.ecommerce.adminservice.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * ADMIN product and inventory management.
 * {@code @AuthenticationPrincipal AuthenticatedUser admin} is built by JwtAuthenticationFilter
 * from the token; admin.userId() is stored as createdBy/updatedBy.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@AuthenticationPrincipal AuthenticatedUser admin,
                                                         @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request, admin.userId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Example: GET /api/v1/admin/products?name=phone&amp;minPrice=1000&amp;page=0&amp;size=10&amp;sort=price,desc
     * Spring builds the Pageable from page/size/sort automatically (max size 50, see application.yml).
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        ProductFilter filter = new ProductFilter(name, categoryId, minPrice, maxPrice, active);
        return ResponseEntity.ok(productService.getProducts(filter, pageable));
    }

    /** Declared before /{id}; Spring prefers the literal path anyway, so "low-stock" is never read as an id. */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@AuthenticationPrincipal AuthenticatedUser admin,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request, admin.userId()));
    }

    /** Soft delete: the product is deactivated, not removed from the database. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProduct(@AuthenticationPrincipal AuthenticatedUser admin,
                                                  @PathVariable Long id) {
        productService.deactivateProduct(id, admin.userId());
        return ResponseEntity.noContent().build();
    }

    /** PATCH = partial update: only the stock changes. */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> adjustStock(@AuthenticationPrincipal AuthenticatedUser admin,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(productService.adjustStock(id, request, admin.userId()));
    }
}
