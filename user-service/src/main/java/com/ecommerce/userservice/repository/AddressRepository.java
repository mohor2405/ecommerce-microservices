package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** All addresses of one user, oldest first. (user.id is navigated via "UserId") */
    List<Address> findByUserIdOrderByIdAsc(Long userId);

    /**
     * Finds an address only if it belongs to the given user. This prevents one user from
     * reading/updating/deleting another user's address just by guessing its id.
     */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findByUserIdAndDefaultAddressTrue(Long userId);

    Optional<Address> findFirstByUserIdOrderByIdAsc(Long userId);

    long countByUserId(Long userId);
}
