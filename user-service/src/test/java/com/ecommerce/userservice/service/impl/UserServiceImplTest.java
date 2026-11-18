package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.request.ChangePasswordRequest;
import com.ecommerce.userservice.dto.request.UpdateUserRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.InvalidPasswordException;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Spy private UserMapper userMapper = new UserMapper();

    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
        user = User.builder().id(1L).firstName("John").lastName("Doe").email("john@example.com")
                .password("$2a$old-hash").phone("9876543210").role(Role.CUSTOMER).build();
    }

    @Test
    @DisplayName("getProfile: returns the user's profile without password")
    void getProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("getProfile: throws ResourceNotFoundException for unknown id")
    void getProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    @DisplayName("updateProfile: updates names and phone")
    void updateProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = userService.updateProfile(1L, new UpdateUserRequest(" Johnny ", "Smith", "+919876543210"));

        assertThat(response.firstName()).isEqualTo("Johnny");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.phone()).isEqualTo("+919876543210");
        assertThat(response.email()).isEqualTo("john@example.com"); // unchanged
    }

    @Test
    @DisplayName("changePassword: encodes and stores the new password")
    void changePassword_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "$2a$old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass123", "$2a$old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$new-hash");

        userService.changePassword(1L, new ChangePasswordRequest("OldPass123", "NewPass123"));

        assertThat(user.getPassword()).isEqualTo("$2a$new-hash");
    }

    @Test
    @DisplayName("changePassword: rejects wrong current password")
    void changePassword_wrongCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", "$2a$old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("WrongPass1", "NewPass123")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Current password is incorrect");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("changePassword: rejects new password equal to the current one")
    void changePassword_samePassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "$2a$old-hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("OldPass123", "OldPass123")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    @DisplayName("deleteAccount: deletes the user")
    void deleteAccount_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteAccount(1L);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteAccount: throws when the user does not exist")
    void deleteAccount_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAccount(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
