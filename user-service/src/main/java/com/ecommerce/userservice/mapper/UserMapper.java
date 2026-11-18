package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.request.UpdateUserRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.entity.User;
import org.springframework.stereotype.Component;

/**
 * Converts between User entities and DTOs, so controllers never see entities and
 * entities never leak sensitive fields (like password) to clients.
 */
@Component
public class UserMapper {

    /** Password, email normalisation and role are set by the service, not copied blindly. */
    public User toEntity(RegisterRequest request) {
        return User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone())
                .build();
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public void updateEntity(User user, UpdateUserRequest request) {
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
    }
}
