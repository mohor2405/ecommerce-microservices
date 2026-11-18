package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.response.AuthResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.DuplicateResourceException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtService;
import com.ecommerce.userservice.security.UserPrincipal;
import com.ecommerce.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * {@code @RequiredArgsConstructor} (Lombok) generates a constructor for all final fields,
 * so Spring injects the dependencies through the constructor (the recommended injection style).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password())); // store hash only
        user.setRole(Role.CUSTOMER);                                   // never trust a role from the client

        User saved = userRepository.save(user);
        log.info("Registered new user id={}", saved.getId());
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Delegates to Spring Security: loads the user via CustomUserDetailsService and compares the
        // BCrypt hash. Throws BadCredentialsException (-> 401) if the email or password is wrong.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(UserPrincipal.from(user));
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), userMapper.toResponse(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
