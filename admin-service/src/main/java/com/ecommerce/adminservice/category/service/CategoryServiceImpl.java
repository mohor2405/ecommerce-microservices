
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

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    // Create a new category
    @Override
    public CategoryResponse createCategory(CategoryRequest request) {

        // CategoryRequest is a record: the accessor is name(), not getName()
        validateUniqueCategoryName(request.name().trim(), null);

        // The mapper sets active = true unless the admin explicitly sent "active": false
        Category category = categoryMapper.toEntity(request);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    // Get all categories
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll(Sort.by("name"))
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // Get category by ID
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {

        Category category = findCategoryById(id);

        return categoryMapper.toResponse(category);
    }

    // Update existing category
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        Category category = findCategoryById(id);

        validateUniqueCategoryName(request.name().trim(), id);

        categoryMapper.updateEntity(category, request);

        // saveAndFlush runs the UPDATE now, so @PreUpdate refreshes updatedAt before we build the response
        Category updatedCategory = categoryRepository.saveAndFlush(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    // Delete category
    @Override
    public void deleteCategory(Long id) {

        Category category = findCategoryById(id);

        boolean hasProducts = productRepository.existsByCategoryId(id);

        if (hasProducts) {
            throw new BusinessException("Category has products");
        }

        categoryRepository.delete(category);
    }

    // Find category or throw exception
    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with id: " + id));
    }

    // Check duplicate category names (case-insensitive)
    private void validateUniqueCategoryName(String name, Long categoryId) {

        boolean exists;

        if (categoryId == null) {
            exists = categoryRepository.existsByNameIgnoreCase(name);
        } else {
            exists = categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId);
        }

        if (exists) {
            throw new DuplicateResourceException("Category name already exists");
        }
    }
}
