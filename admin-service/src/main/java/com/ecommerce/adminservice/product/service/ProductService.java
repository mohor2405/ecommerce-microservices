package com.ecommerce.adminservice.product.service;

import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.product.dto.request.CreateProductRequest;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.request.StockUpdateRequest;
import com.ecommerce.adminservice.product.dto.request.UpdateProductRequest;
import com.ecommerce.adminservice.product.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Product management for ADMIN users. adminId = userId from the caller's JWT. */
public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request, Long adminId);

    PageResponse<ProductResponse> getProducts(ProductFilter filter, Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, UpdateProductRequest request, Long adminId);

    /** Soft delete: sets active = false. */
    void deactivateProduct(Long id, Long adminId);

    ProductResponse adjustStock(Long id, StockUpdateRequest request, Long adminId);

    List<ProductResponse> getLowStockProducts(int threshold);
}
