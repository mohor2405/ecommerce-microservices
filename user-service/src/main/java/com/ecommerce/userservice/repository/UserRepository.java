package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation at runtime.
 * Query methods are derived from method names, e.g. findByEmail -> WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
