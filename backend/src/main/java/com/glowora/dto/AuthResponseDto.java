package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private CustomerResponseDto customer;
}
