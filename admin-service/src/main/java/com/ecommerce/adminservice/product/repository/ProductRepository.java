package com.ecommerce.adminservice.product.repository;

import com.ecommerce.adminservice.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaSpecificationExecutor adds findAll(Specification, Pageable): dynamic WHERE clauses built
 * from optional filters (see ProductSpecifications).
 *
 * <p>@EntityGraph(attributePaths = "category") loads the category in the SAME query (a join).
 * Without it, mapping a page of 10 products would run 1 query for the products plus 1 per
 * product to load its category: the classic "N+1 queries" problem.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Used by CategoryServiceImpl: a category cannot be deleted while products reference it. */
    boolean existsByCategoryId(Long categoryId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    /** "With" is just a readable word: Spring Data ignores text between "find" and "By". */
    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(int threshold);
}
