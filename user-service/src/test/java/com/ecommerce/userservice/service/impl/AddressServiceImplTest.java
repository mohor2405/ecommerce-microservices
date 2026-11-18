package com.ecommerce.userservice.service.impl;

import com.ecommerce.userservice.dto.request.AddressRequest;
import com.ecommerce.userservice.dto.response.AddressResponse;
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.AddressMapper;
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Spy private AddressMapper addressMapper = new AddressMapper();

    private AddressServiceImpl addressService;
    private User user;

    @BeforeEach
    void setUp() {
        addressService = new AddressServiceImpl(addressRepository, userRepository, addressMapper);
        user = User.builder().id(1L).email("john@example.com").role(Role.CUSTOMER).build();
    }

    private static AddressRequest request(Boolean makeDefault) {
        return new AddressRequest("221B Baker Street", null, "London", "Greater London", "NW1 6XE", "UK", makeDefault);
    }

    private Address address(long id, boolean isDefault) {
        return Address.builder().id(id).user(user).addressLine1("Line " + id).city("City").state("State")
                .postalCode("12345").country("IN").defaultAddress(isDefault).build();
    }

    private void saveReturnsArgumentWithId(long id) {
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(id);
            return a;
        });
    }

    @Test
    @DisplayName("addAddress: the first address automatically becomes the default")
    void addAddress_firstBecomesDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(0L);
        when(addressRepository.findByUserIdAndDefaultAddressTrue(1L)).thenReturn(Optional.empty());
        saveReturnsArgumentWithId(10L);

        AddressResponse response = addressService.addAddress(1L, request(null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.defaultAddress()).isTrue();
        assertThat(response.city()).isEqualTo("London");
    }

    @Test
    @DisplayName("addAddress: a new default address un-defaults the previous one")
    void addAddress_newDefaultReplacesOld() {
        Address oldDefault = address(10L, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        when(addressRepository.findByUserIdAndDefaultAddressTrue(1L)).thenReturn(Optional.of(oldDefault));
        saveReturnsArgumentWithId(11L);

        AddressResponse response = addressService.addAddress(1L, request(true));

        assertThat(response.defaultAddress()).isTrue();
        assertThat(oldDefault.isDefaultAddress()).isFalse();
    }

    @Test
    @DisplayName("addAddress: a non-default second address leaves the existing default alone")
    void addAddress_nonDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        saveReturnsArgumentWithId(11L);

        AddressResponse response = addressService.addAddress(1L, request(false));

        assertThat(response.defaultAddress()).isFalse();
        verify(addressRepository, never()).findByUserIdAndDefaultAddressTrue(any());
    }

    @Test
    @DisplayName("addAddress: throws when user does not exist")
    void addAddress_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.addAddress(1L, request(null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAddresses: maps all of the user's addresses")
    void getAddresses() {
        when(addressRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(address(10L, true), address(11L, false)));

        List<AddressResponse> result = addressService.getAddresses(1L);

        assertThat(result).extracting(AddressResponse::id).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("updateAddress: another user's address is reported as not found")
    void updateAddress_notOwned() {
        when(addressRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(1L, 50L, request(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 50");
    }

    @Test
    @DisplayName("updateAddress: updates fields and can make the address default")
    void updateAddress_success() {
        Address target = address(11L, false);
        Address oldDefault = address(10L, true);
        when(addressRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(target));
        when(addressRepository.findByUserIdAndDefaultAddressTrue(1L)).thenReturn(Optional.of(oldDefault));
        when(addressRepository.saveAndFlush(target)).thenReturn(target);

        AddressResponse response = addressService.updateAddress(1L, 11L, request(true));

        assertThat(response.addressLine1()).isEqualTo("221B Baker Street");
        assertThat(response.defaultAddress()).isTrue();
        assertThat(oldDefault.isDefaultAddress()).isFalse();
    }

    @Test
    @DisplayName("deleteAddress: deleting the default promotes the oldest remaining address")
    void deleteAddress_promotesNextDefault() {
        Address toDelete = address(10L, true);
        Address remaining = address(11L, false);
        when(addressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(toDelete));
        when(addressRepository.findFirstByUserIdOrderByIdAsc(1L)).thenReturn(Optional.of(remaining));

        addressService.deleteAddress(1L, 10L);

        verify(addressRepository).delete(toDelete);
        assertThat(remaining.isDefaultAddress()).isTrue();
    }

    @Test
    @DisplayName("deleteAddress: deleting a non-default address does not touch the default")
    void deleteAddress_nonDefault() {
        Address toDelete = address(11L, false);
        when(addressRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(toDelete));

        addressService.deleteAddress(1L, 11L);

        verify(addressRepository).delete(toDelete);
        verify(addressRepository, never()).findFirstByUserIdOrderByIdAsc(any());
    }
}
