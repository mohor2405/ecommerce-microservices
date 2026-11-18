package com.ecommerce.adminservice.product.mapper;

import com.ecommerce.adminservice.category.entity.Category;
import com.ecommerce.adminservice.product.dto.request.CreateProductRequest;
import com.ecommerce.adminservice.product.dto.request.UpdateProductRequest;
import com.ecommerce.adminservice.product.dto.response.CatalogProductResponse;
import com.ecommerce.adminservice.product.dto.response.CategorySummary;
import com.ecommerce.adminservice.product.dto.response.ProductResponse;
import com.ecommerce.adminservice.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Entity &lt;-&gt; DTO conversion. Category, SKU uniqueness and audit fields
 * (createdBy/updatedBy) are decided by the service; the mapper only copies data.
 */
@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request) {
        Product product = new Product();
        product.setSku(normalizeSku(request.sku()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        return product;
    }

    public void updateEntity(Product product, UpdateProductRequest request) {
        product.setSku(normalizeSku(request.sku()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl());
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.isActive(),
                toSummary(product.getCategory()),
                product.getCreatedBy(),
                product.getUpdatedBy(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public CatalogProductResponse toCatalogResponse(Product product) {
        return new CatalogProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity() > 0,
                product.getImageUrl(),
                toSummary(product.getCategory())
        );
    }

    /** "ph-001 " -> "PH-001" */
    public static String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private CategorySummary toSummary(Category category) {
        return new CategorySummary(category.getId(), category.getName());
    }
}
