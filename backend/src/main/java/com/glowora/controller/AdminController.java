package com.glowora.controller;

import com.glowora.dto.*;
import com.glowora.service.AuthService;
import com.glowora.service.CustomerService;
import com.glowora.service.DashboardService;
import com.glowora.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final DashboardService dashboardService;
    private final CustomerService customerService;
    private final ProductService productService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> adminLogin(@Valid @RequestBody LoginRequestDto requestDto,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        AuthResponseDto authResponse = authService.adminLogin(requestDto, request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> adminLogout(HttpServletRequest request,
                                                           HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Administrator logged out successfully."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponseDto> getCurrentAdmin() {
        return ResponseEntity.ok(authService.getCurrentCustomer());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponseDto>> getCustomers(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(customerService.getAllCustomersForAdmin(keyword));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<AdminCustomerDetailDto> getCustomerDetails(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerDetailForAdmin(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(requestDto));
    }
}
