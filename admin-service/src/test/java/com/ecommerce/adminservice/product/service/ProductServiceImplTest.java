package com.ecommerce.adminservice.product.service;

import com.ecommerce.adminservice.category.entity.Category;
import com.ecommerce.adminservice.category.repository.CategoryRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Long ADMIN_ID = 42L;

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Spy private ProductMapper productMapper = new ProductMapper();

    @InjectMocks private ProductServiceImpl productService;

    private Category electronics;
    private Product phone;

    @BeforeEach
    void setUp() {
        electronics = Category.builder().id(1L).name("Electronics").active(true).build();
        phone = Product.builder().id(10L).sku("PH-001").name("Phone").price(new BigDecimal("999.00"))
                .stockQuantity(5).category(electronics).active(true).createdBy(1L).updatedBy(1L).build();
    }

    private static CreateProductRequest createRequest(String sku, Long categoryId) {
        return new CreateProductRequest(sku, "Smartphone X", "128 GB", new BigDecimal("24999.00"), 50, categoryId, null);
    }

    @Test
    @DisplayName("createProduct: uppercases SKU, links category, records the admin")
    void create_success() {
        when(productRepository.existsBySku("PH-002")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });

        ProductResponse response = productService.createProduct(createRequest(" ph-002 ", 1L), ADMIN_ID);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.sku()).isEqualTo("PH-002");
        assertThat(response.category().name()).isEqualTo("Electronics");
        assertThat(response.createdBy()).isEqualTo(ADMIN_ID);
        assertThat(response.updatedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("createProduct: duplicate SKU -> DuplicateResourceException")
    void create_duplicateSku() {
        when(productRepository.existsBySku("PH-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(createRequest("ph-001", 1L), ADMIN_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("PH-001");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct: unknown category -> ResourceNotFoundException")
    void create_unknownCategory() {
        when(productRepository.existsBySku("PH-002")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(createRequest("PH-002", 99L), ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 99");
    }

    @Test
    @DisplayName("updateProduct: moves product to another category and keeps stock unchanged")
    void update_changesCategory() {
        Category books = Category.builder().id(2L).name("Books").active(true).build();
        when(productRepository.findWithCategoryById(10L)).thenReturn(Optional.of(phone));
        when(productRepository.existsBySkuAndIdNot("PH-001", 10L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(books));
        when(productRepository.saveAndFlush(phone)).thenReturn(phone);

        ProductResponse response = productService.updateProduct(10L,
                new UpdateProductRequest("ph-001", "Phone Book", null, new BigDecimal("10.50"), 2L, null, null),
                ADMIN_ID);

        assertThat(response.category().id()).isEqualTo(2L);
        assertThat(response.price()).isEqualByComparingTo("10.50");
        assertThat(response.stockQuantity()).isEqualTo(5);
        assertThat(response.updatedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.createdBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deactivateProduct: soft delete sets active=false, never deletes the row")
    void deactivate() {
        when(productRepository.findWithCategoryById(10L)).thenReturn(Optional.of(phone));

        productService.deactivateProduct(10L, ADMIN_ID);

        assertThat(phone.isActive()).isFalse();
        assertThat(phone.getUpdatedBy()).isEqualTo(ADMIN_ID);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("adjustStock: positive change adds stock")
    void adjustStock_add() {
        when(productRepository.findWithCategoryById(10L)).thenReturn(Optional.of(phone));
        when(productRepository.saveAndFlush(phone)).thenReturn(phone);

        ProductResponse response = productService.adjustStock(10L, new StockUpdateRequest(10), ADMIN_ID);

        assertThat(response.stockQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("adjustStock: going below zero -> BadRequestException, stock unchanged")
    void adjustStock_insufficient() {
        when(productRepository.findWithCategoryById(10L)).thenReturn(Optional.of(phone));

        assertThatThrownBy(() -> productService.adjustStock(10L, new StockUpdateRequest(-6), ADMIN_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(phone.getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("adjustStock: zero change is rejected")
    void adjustStock_zero() {
        assertThatThrownBy(() -> productService.adjustStock(10L, new StockUpdateRequest(0), ADMIN_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getProducts: minPrice > maxPrice -> BadRequestException")
    void getProducts_invalidPriceRange() {
        ProductFilter filter = new ProductFilter(null, null, new BigDecimal("500"), new BigDecimal("100"), null);

        assertThatThrownBy(() -> productService.getProducts(filter, Pageable.unpaged()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getLowStockProducts: maps results and rejects a negative threshold")
    void lowStock() {
        when(productRepository.findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of(phone));

        assertThat(productService.getLowStockProducts(5)).extracting(ProductResponse::sku).containsExactly("PH-001");
        assertThatThrownBy(() -> productService.getLowStockProducts(-1)).isInstanceOf(BadRequestException.class);
    }
}
