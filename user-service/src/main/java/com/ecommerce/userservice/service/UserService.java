package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.request.ChangePasswordRequest;
import com.ecommerce.userservice.dto.request.UpdateUserRequest;
import com.ecommerce.userservice.dto.response.UserResponse;

/** Profile operations for an existing user. */
public interface UserService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateUserRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void deleteAccount(Long userId);
}
