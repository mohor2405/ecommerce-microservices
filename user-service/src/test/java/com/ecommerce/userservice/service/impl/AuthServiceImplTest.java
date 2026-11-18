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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring context, no database. Every dependency is a Mockito mock,
 * except UserMapper which is a real object (@Spy) because it has no dependencies of its own.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Spy private UserMapper userMapper = new UserMapper();

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager, jwtService, userMapper);
    }

    @Test
    @DisplayName("register: saves user with encoded password and CUSTOMER role, returns token")
    void register_success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "  John.Doe@Example.com ", "Secret123", "9876543210");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");   // normalised
        assertThat(saved.getPassword()).isEqualTo("$2a$hashed");           // never plain text
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email already exists")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "Secret123", null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("login: authenticates and returns token for valid credentials")
    void login_success() {
        User user = User.builder().id(5L).firstName("Jane").lastName("Roe")
                .email("jane@example.com").password("$2a$hash").role(Role.CUSTOMER).build();
        UserPrincipal principal = UserPrincipal.from(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("Jane@Example.com", "Secret123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(5L);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException for wrong credentials")
    void login_badCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }
}
