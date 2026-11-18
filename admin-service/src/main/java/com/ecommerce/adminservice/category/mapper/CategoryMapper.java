package com.ecommerce.adminservice.category.mapper;

import com.ecommerce.adminservice.category.dto.request.CategoryRequest;
import com.ecommerce.adminservice.category.dto.response.CategoryResponse;
import com.ecommerce.adminservice.category.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Converts between Category entity and DTOs.
 * A Spring @Component (instead of static methods) so it can be injected into the service
 * and replaced/spied in unit tests, the same style as the User Service mappers.
 */
@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());
        return category;
    }

    /** Update only changes "active" when the client actually sent it. */
    public void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
