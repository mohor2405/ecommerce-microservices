package com.ecommerce.adminservice.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating/updating a category.
 * A record generates the constructor and accessors for us: request.name(), request.description()...
 *
 * @param active optional: null means "true" on create and "unchanged" on update
 */
public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        Boolean active
) {
}
