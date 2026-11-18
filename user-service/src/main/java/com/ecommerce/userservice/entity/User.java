package com.ecommerce.userservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity mapped to the {@code users} table.
 *
 * <p>Note: we deliberately avoid Lombok's {@code @Data} on entities. Its generated
 * equals/hashCode/toString would touch the lazy {@code addresses} collection, which can
 * cause infinite recursion (User -> Address -> User ...) and unexpected SQL queries.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor   // required by JPA
@AllArgsConstructor  // required by @Builder
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String email;

    /** BCrypt hash, never the plain-text password. Never exposed in any response DTO. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * One user has many addresses. {@code mappedBy = "user"} means the Address table owns the
     * foreign key. CascadeType.ALL + orphanRemoval: deleting a user deletes their addresses.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA lifecycle callback: runs just before the INSERT statement. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** JPA lifecycle callback: runs just before an UPDATE statement. */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
