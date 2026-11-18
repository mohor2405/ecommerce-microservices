package com.ecommerce.adminservice.product.service;

import com.ecommerce.adminservice.category.entity.Category;
import com.ecommerce.adminservice.category.repository.CategoryRepository;
import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.exception.BadRequestException;
import com.ecommerce.adminservice.exception.DuplicateResourceException;
import com.ecommerce.adminservice.exception.ResourceNotFoundException;
import com.ecommerce.adminservice.product.dto.request.CreateProductRequest;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.request.StockUpdateRequest;
import com.ecommerce.adminservice.product.dto.request.UpdateProductRequest;
import com.ecommerce.adminservice.product.dto.response.ProductResponse;
import com.ecommerce.adminservice.product.entity.Product;
import com.ecommerce.adminservice.product.mapper.ProductMapper;
import com.ecommerce.adminservice.product.repository.ProductRepository;
import com.ecommerce.adminservice.product.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business rules for products:
 * <ul>
 *   <li>SKU is unique (stored uppercase).</li>
 *   <li>Every product must belong to an existing category.</li>
 *   <li>Delete = soft delete (active = false).</li>
 *   <li>Stock never goes below zero, and changes only through adjustStock().</li>
 *   <li>createdBy/updatedBy record which admin made the change.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(CreateProductRequest request, Long adminId) {
        String sku = ProductMapper.normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product with SKU " + sku + " already exists");
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(findCategory(request.categoryId()));
        product.setCreatedBy(adminId);
        product.setUpdatedBy(adminId);

        Product saved = productRepository.save(product);
        log.info("Admin {} created product id={} sku={}", adminId, saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductFilter filter, Pageable pageable) {
        if (filter.hasInvalidPriceRange()) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.from(filter), pageable),
                productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    public ProductResponse updateProduct(Long id, UpdateProductRequest request, Long adminId) {
        Product product = findProduct(id);

        String sku = ProductMapper.normalizeSku(request.sku());
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException("Product with SKU " + sku + " already exists");
        }

        productMapper.updateEntity(product, request);
        if (!product.getCategory().getId().equals(request.categoryId())) {
            product.setCategory(findCategory(request.categoryId()));
        }
        product.setUpdatedBy(adminId);

        return productMapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Override
    public void deactivateProduct(Long id, Long adminId) {
        Product product = findProduct(id);
        product.setActive(false);
        product.setUpdatedBy(adminId);
        log.info("Admin {} deactivated product id={}", adminId, id);
        // No save() call needed: the entity is managed, so the change is written at commit (dirty checking).
    }

    @Override
    public ProductResponse adjustStock(Long id, StockUpdateRequest request, Long adminId) {
        int change = request.quantityChange();
        if (change == 0) {
            throw new BadRequestException("Quantity change must not be zero");
        }

        Product product = findProduct(id);
        int newStock = product.getStockQuantity() + change;
        if (newStock < 0) {
            throw new BadRequestException("Insufficient stock: available %d, requested change %d"
                    .formatted(product.getStockQuantity(), change));
        }

        product.setStockQuantity(newStock);
        product.setUpdatedBy(adminId);
        return productMapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        if (threshold < 0) {
            throw new BadRequestException("Threshold must not be negative");
        }
        return productRepository.findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    private Product findProduct(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }
}
