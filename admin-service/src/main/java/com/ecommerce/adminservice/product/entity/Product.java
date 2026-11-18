package com.ecommerce.adminservice.product.entity;

import com.ecommerce.adminservice.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code products} table. Many products belong to one category.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
        indexes = {
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_name", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning side of the relationship: holds the category_id foreign key. No cascade on purpose. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    /** Stored in UPPERCASE so "ph-001" and "PH-001" are treated as the same SKU. */
    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Money is always BigDecimal: double cannot represent values like 0.1 exactly. DECIMAL(10,2). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Soft delete flag: products are deactivated, never physically deleted (orders will reference them). */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Admin userId from the User Service JWT. Plain number, NO foreign key (it lives in another service's DB). */
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    /**
     * Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE. If two admins change
     * the same product at the same time, the second update fails instead of silently overwriting
     * the first (important for stock quantities).
     */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
