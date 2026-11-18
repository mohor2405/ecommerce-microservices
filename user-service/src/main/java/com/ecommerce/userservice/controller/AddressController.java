package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.request.AddressRequest;
import com.ecommerce.userservice.dto.response.AddressResponse;
import com.ecommerce.userservice.security.UserPrincipal;
import com.ecommerce.userservice.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** CRUD endpoints for the logged-in user's addresses. */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /** Returns 201 Created with a Location header pointing to the new address (REST convention). */
    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.addAddress(principal.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getAddresses(principal.getId()));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddress(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddress(principal.getId(), addressId));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long addressId,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(principal.getId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long addressId) {
        addressService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
