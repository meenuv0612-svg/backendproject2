package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponseDto {
    private Long id;
    private Long customerId;
    private String label;
    private String recipientName;
    private String phone;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
