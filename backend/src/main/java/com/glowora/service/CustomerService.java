package com.glowora.service;

import com.glowora.dto.AddressRequestDto;
import com.glowora.dto.AddressResponseDto;
import com.glowora.dto.AdminCustomerDetailDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.CustomerUpdateRequestDto;

import java.util.List;

public interface CustomerService {
    CustomerResponseDto getCustomerById(Long customerId);
    CustomerResponseDto updateCustomer(Long customerId, CustomerUpdateRequestDto requestDto);
    List<AddressResponseDto> getAddresses(Long customerId);
    AddressResponseDto addAddress(Long customerId, AddressRequestDto requestDto);
    AddressResponseDto updateAddress(Long customerId, Long addressId, AddressRequestDto requestDto);
    void deleteAddress(Long customerId, Long addressId);
    List<CustomerResponseDto> getAllCustomersForAdmin(String keyword);
    AdminCustomerDetailDto getCustomerDetailForAdmin(Long customerId);
}

