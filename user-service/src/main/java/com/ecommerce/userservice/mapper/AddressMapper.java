package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dto.request.AddressRequest;
import com.ecommerce.userservice.dto.response.AddressResponse;
import com.ecommerce.userservice.entity.Address;
import org.springframework.stereotype.Component;

/** Converts between Address entities and DTOs. The "default" flag is handled by the service. */
@Component
public class AddressMapper {

    public Address toEntity(AddressRequest request) {
        Address address = new Address();
        updateEntity(address, request);
        return address;
    }

    public void updateEntity(Address address, AddressRequest request) {
        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
    }

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
