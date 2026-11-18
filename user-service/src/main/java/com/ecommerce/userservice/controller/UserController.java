package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.request.ChangePasswordRequest;
import com.ecommerce.userservice.dto.request.UpdateUserRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.security.UserPrincipal;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile endpoints for the logged-in user.
 *
 * <p>We use "/me" instead of "/users/{id}" for self-service operations: the user id comes from
 * the validated JWT ({@code @AuthenticationPrincipal}), so a user can never act on someone
 * else's account by changing an id in the URL.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Role-based authorization example: only ADMIN users may look up any user by id.
     * (A future Admin Service / Order Service will rely on this kind of endpoint.)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }
}
