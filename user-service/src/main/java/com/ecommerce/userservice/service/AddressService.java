package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.request.AddressRequest;
import com.ecommerce.userservice.dto.response.AddressResponse;

import java.util.List;

/** Address management. Every method is scoped to the owning user's id. */
public interface AddressService {

    AddressResponse addAddress(Long userId, AddressRequest request);

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse getAddress(Long userId, Long addressId);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);
}
