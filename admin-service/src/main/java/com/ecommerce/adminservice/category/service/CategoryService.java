package com.ecommerce.adminservice.category.service;

import java.util.List;

import com.ecommerce.adminservice.category.dto.request.CategoryRequest;
import com.ecommerce.adminservice.category.dto.response.CategoryResponse;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
