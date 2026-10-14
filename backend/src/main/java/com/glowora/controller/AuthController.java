package com.glowora.controller;

import com.glowora.dto.AuthResponseDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.LoginRequestDto;
import com.glowora.dto.RegisterRequestDto;
import com.glowora.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        AuthResponseDto authResponse = authService.register(requestDto, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        AuthResponseDto authResponse = authService.login(requestDto, request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponseDto> adminLogin(@Valid @RequestBody LoginRequestDto requestDto,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        AuthResponseDto authResponse = authService.adminLogin(requestDto, request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponseDto> getCurrentCustomer() {
        return ResponseEntity.ok(authService.getCurrentCustomer());
    }
}

