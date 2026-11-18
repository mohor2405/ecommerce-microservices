package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;

/**
 * Registration and login. Controllers depend on this interface, not on the implementation
 * (Dependency Inversion), which also makes them easy to test with mocks.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
