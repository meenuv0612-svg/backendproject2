package com.glowora.service.impl;

import com.glowora.dto.AddressRequestDto;
import com.glowora.dto.AddressResponseDto;
import com.glowora.dto.AdminCustomerDetailDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.CustomerUpdateRequestDto;
import com.glowora.dto.OrderResponseDto;
import com.glowora.entity.Customer;
import com.glowora.entity.CustomerAddress;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CustomerAddressRepository;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.OrderRepository;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final EntityMapper entityMapper;

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        Customer customer = findCustomerOrThrow(customerId);
        return entityMapper.toCustomerDto(customer);
    }

    @Override
    @Transactional
    public CustomerResponseDto updateCustomer(Long customerId, CustomerUpdateRequestDto requestDto) {
        ownershipValidator.validateOwnership(customerId);
        Customer customer = findCustomerOrThrow(customerId);
        customer.setFullName(requestDto.getFullName().trim());
        customer.setPhone(requestDto.getPhone().trim());
        Customer saved = customerRepository.save(customer);
        return entityMapper.toCustomerDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAddresses(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        findCustomerOrThrow(customerId);
        return addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId)
                .stream()
                .map(entityMapper::toAddressDto)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponseDto addAddress(Long customerId, AddressRequestDto requestDto) {
        ownershipValidator.validateOwnership(customerId);
        Customer customer = findCustomerOrThrow(customerId);

        List<CustomerAddress> existing = addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId);
        boolean makeDefault = Boolean.TRUE.equals(requestDto.getIsDefault()) || existing.isEmpty();

        if (makeDefault && !existing.isEmpty()) {
            for (CustomerAddress addr : existing) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .label(requestDto.getLabel() != null && !requestDto.getLabel().isBlank() ? requestDto.getLabel().trim() : "Home")
                .recipientName(requestDto.getRecipientName().trim())
                .phone(requestDto.getPhone().trim())
                .streetAddress(requestDto.getStreetAddress().trim())
                .city(requestDto.getCity().trim())
                .state(requestDto.getState().trim())
                .postalCode(requestDto.getPostalCode().trim())
                .isDefault(makeDefault)
                .build();

        CustomerAddress saved = addressRepository.save(address);
        return entityMapper.toAddressDto(saved);
    }

    @Override
    @Transactional
    public AddressResponseDto updateAddress(Long customerId, Long addressId, AddressRequestDto requestDto) {
        ownershipValidator.validateOwnership(customerId);
        findCustomerOrThrow(customerId);

        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));

        if (Boolean.TRUE.equals(requestDto.getIsDefault())) {
            List<CustomerAddress> existing = addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId);
            for (CustomerAddress addr : existing) {
                if (!addr.getId().equals(addressId) && Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
            address.setIsDefault(true);
        } else if (requestDto.getIsDefault() != null) {
            address.setIsDefault(false);
        }

        if (requestDto.getLabel() != null && !requestDto.getLabel().isBlank()) {
            address.setLabel(requestDto.getLabel().trim());
        }
        address.setRecipientName(requestDto.getRecipientName().trim());
        address.setPhone(requestDto.getPhone().trim());
        address.setStreetAddress(requestDto.getStreetAddress().trim());
        address.setCity(requestDto.getCity().trim());
        address.setState(requestDto.getState().trim());
        address.setPostalCode(requestDto.getPostalCode().trim());

        CustomerAddress saved = addressRepository.save(address);
        return entityMapper.toAddressDto(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        ownershipValidator.validateOwnership(customerId);
        findCustomerOrThrow(customerId);

        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));
        addressRepository.delete(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomersForAdmin(String keyword) {
        List<Customer> customers;
        if (keyword != null && !keyword.trim().isEmpty()) {
            customers = customerRepository.searchCustomersByKeyword(keyword.trim());
        } else {
            customers = customerRepository.findByRoleOrderByCreatedAtDesc("ROLE_CUSTOMER");
        }
        return customers.stream()
                .map(entityMapper::toCustomerDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCustomerDetailDto getCustomerDetailForAdmin(Long customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        List<AddressResponseDto> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId)
                .stream()
                .map(entityMapper::toAddressDto)
                .toList();
        List<OrderResponseDto> recentOrders = orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(entityMapper::toOrderDto)
                .toList();

        return AdminCustomerDetailDto.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .totalOrders(recentOrders.size())
                .addresses(addresses)
                .recentOrders(recentOrders)
                .build();
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }
}

