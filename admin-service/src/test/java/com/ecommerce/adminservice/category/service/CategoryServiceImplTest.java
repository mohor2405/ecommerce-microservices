package com.ecommerce.adminservice.category.service;

import com.ecommerce.adminservice.category.dto.request.CategoryRequest;
import com.ecommerce.adminservice.category.dto.response.CategoryResponse;
import com.ecommerce.adminservice.category.entity.Category;
import com.ecommerce.adminservice.category.mapper.CategoryMapper;
import com.ecommerce.adminservice.category.repository.CategoryRepository;
import com.ecommerce.adminservice.exception.BusinessException;
import com.ecommerce.adminservice.exception.DuplicateResourceException;
import com.ecommerce.adminservice.exception.ResourceNotFoundException;
import com.ecommerce.adminservice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @InjectMocks builds CategoryServiceImpl with the @Mock / @Spy fields below. */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Spy private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks private CategoryServiceImpl categoryService;

    private static Category category(long id, String name) {
        return Category.builder().id(id).name(name).active(true).build();
    }

    @Test
    @DisplayName("createCategory: saves a trimmed, active category")
    void create_success() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(new CategoryRequest("  Electronics ", "Gadgets", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("createCategory: explicit active=false is respected")
    void create_inactive() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.createCategory(new CategoryRequest("Seasonal", null, false));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("createCategory: duplicate name (any case) -> DuplicateResourceException")
    void create_duplicate() {
        when(categoryRepository.existsByNameIgnoreCase("electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(new CategoryRequest("electronics", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCategoryById: unknown id -> ResourceNotFoundException")
    void get_notFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 9");
    }

    @Test
    @DisplayName("updateCategory: duplicate check excludes the category itself")
    void update_success() {
        Category existing = category(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Electronics & Gadgets", 1L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse response = categoryService.updateCategory(1L,
                new CategoryRequest("Electronics & Gadgets", "All gadgets", false));

        assertThat(response.name()).isEqualTo("Electronics & Gadgets");
        assertThat(response.description()).isEqualTo("All gadgets");
        assertThat(response.active()).isFalse();
    }

    @Test
    @DisplayName("updateCategory: name used by another category -> DuplicateResourceException")
    void update_duplicate() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Books", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, new CategoryRequest("Books", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("deleteCategory: category with products -> BusinessException, nothing deleted")
    void delete_withProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Category has products");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCategory: empty category is deleted")
    void delete_success() {
        Category existing = category(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(existing);
    }
}
