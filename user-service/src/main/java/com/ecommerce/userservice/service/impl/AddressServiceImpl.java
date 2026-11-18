package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.request.AddressRequest;
import com.ecommerce.userservice.dto.response.AddressResponse;
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.AddressMapper;
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business rules for addresses:
 * <ul>
 *   <li>A user's first address automatically becomes the default.</li>
 *   <li>At most one default address per user: making one default un-defaults the previous one.</li>
 *   <li>Deleting the default address promotes the user's oldest remaining address.</li>
 *   <li>Users can only access their own addresses (looked up by id AND user id).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        boolean makeDefault = isFirstAddress || Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault) {
            clearCurrentDefault(userId);
        }

        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setDefaultAddress(makeDefault);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long userId, Long addressId) {
        return addressMapper.toResponse(findOwnedAddress(userId, addressId));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findOwnedAddress(userId, addressId);
        addressMapper.updateEntity(address, request);

        // To change the default, mark ANOTHER address as default. Sending false here is ignored so
        // that a user always keeps exactly one default address.
        if (Boolean.TRUE.equals(request.defaultAddress()) && !address.isDefaultAddress()) {
            clearCurrentDefault(userId);
            address.setDefaultAddress(true);
        }
        return addressMapper.toResponse(addressRepository.saveAndFlush(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);

        if (wasDefault) {
            // Hibernate flushes the DELETE before this query runs, so the deleted row is not returned.
            addressRepository.findFirstByUserIdOrderByIdAsc(userId)
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private Address findOwnedAddress(Long userId, Long addressId) {
        // Returns 404 (not 403) for other users' addresses so we don't reveal that the id exists.
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private void clearCurrentDefault(Long userId) {
        addressRepository.findByUserIdAndDefaultAddressTrue(userId)
                .ifPresent(current -> current.setDefaultAddress(false));
    }
}
