package com.ecommerce.adminservice.catalog.service;

import com.ecommerce.adminservice.category.dto.response.CategoryResponse;
import com.ecommerce.adminservice.category.mapper.CategoryMapper;
import com.ecommerce.adminservice.category.repository.CategoryRepository;
import com.ecommerce.adminservice.dto.response.PageResponse;
import com.ecommerce.adminservice.exception.BadRequestException;
import com.ecommerce.adminservice.exception.ResourceNotFoundException;
import com.ecommerce.adminservice.product.dto.request.ProductFilter;
import com.ecommerce.adminservice.product.dto.response.CatalogProductResponse;
import com.ecommerce.adminservice.product.entity.Product;
import com.ecommerce.adminservice.product.mapper.ProductMapper;
import com.ecommerce.adminservice.product.repository.ProductRepository;
import com.ecommerce.adminservice.product.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<CatalogProductResponse> getProducts(ProductFilter filter, Pageable pageable) {
        if (filter.hasInvalidPriceRange()) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
        // Whatever the client sends, the public catalog only ever shows active products in active categories.
        ProductFilter publicFilter = new ProductFilter(
                filter.name(), filter.categoryId(), filter.minPrice(), filter.maxPrice(), true);
        Specification<Product> spec = ProductSpecifications.from(publicFilter)
                .and(ProductSpecifications.inActiveCategory());

        return PageResponse.from(productRepository.findAll(spec, pageable), productMapper::toCatalogResponse);
    }

    @Override
    public CatalogProductResponse getProduct(Long id) {
        return productRepository.findWithCategoryById(id)
                .filter(p -> p.isActive() && p.getCategory().isActive())
                // Hidden products look exactly like missing ones: customers can't tell the difference.
                .map(productMapper::toCatalogResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
