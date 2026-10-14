package com.glowora.controller;

import com.glowora.dto.AddressRequestDto;
import com.glowora.dto.AddressResponseDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.CustomerUpdateRequestDto;
import com.glowora.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(@PathVariable Long id,
                                                              @Valid @RequestBody CustomerUpdateRequestDto requestDto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, requestDto));
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressResponseDto>> getAddresses(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getAddresses(id));
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressResponseDto> addAddress(@PathVariable Long id,
                                                         @Valid @RequestBody AddressRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.addAddress(id, requestDto));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<AddressResponseDto> updateAddress(@PathVariable Long id,
                                                            @PathVariable Long addressId,
                                                            @Valid @RequestBody AddressRequestDto requestDto) {
        return ResponseEntity.ok(customerService.updateAddress(id, addressId, requestDto));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<Map<String, Object>> deleteAddress(@PathVariable Long id,
                                                             @PathVariable Long addressId) {
        customerService.deleteAddress(id, addressId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Address deleted successfully."
        ));
    }
}
