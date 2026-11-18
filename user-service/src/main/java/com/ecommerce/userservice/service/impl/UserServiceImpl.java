package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.request.ChangePasswordRequest;
import com.ecommerce.userservice.dto.request.UpdateUserRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.InvalidPasswordException;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional}: each public method runs in one DB transaction. Changes made to a
 * loaded (managed) entity are saved automatically at commit ("dirty checking"), so no
 * explicit save() call is needed after modifying it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        userMapper.updateEntity(user, request);
        // saveAndFlush so the @PreUpdate timestamp is applied before we build the response
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        log.info("Password changed for user id={}", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);
        userRepository.delete(user); // cascades to the user's addresses
        log.info("Deleted user id={}", userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
