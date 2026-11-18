
package com.ecommerce.adminservice.category.repository;

import com.ecommerce.adminservice.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Check whether a category name already exists (case-insensitive)
    boolean existsByNameIgnoreCase(String name);

    // Check duplicate name while excluding the current category
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Public catalog: only active categories, alphabetically
    List<Category> findByActiveTrueOrderByNameAsc();

}
