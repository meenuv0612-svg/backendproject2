package com.glowora.service;

import com.glowora.dto.AuthResponseDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.LoginRequestDto;
import com.glowora.dto.RegisterRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto requestDto, HttpServletRequest request, HttpServletResponse response);
    AuthResponseDto login(LoginRequestDto requestDto, HttpServletRequest request, HttpServletResponse response);
    AuthResponseDto adminLogin(LoginRequestDto requestDto, HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    CustomerResponseDto getCurrentCustomer();
}

