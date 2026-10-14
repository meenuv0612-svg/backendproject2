package com.glowora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDto {

    @Size(max = 60, message = "Label cannot exceed 60 characters")
    private String label;

    @NotBlank(message = "Recipient name is required")
    @Size(min = 2, max = 120, message = "Recipient name must be between 2 and 120 characters")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s-]{10,15}$", message = "Phone number must be 10 to 15 digits")
    private String phone;

    @NotBlank(message = "Street address is required")
    @Size(min = 5, max = 350, message = "Street address must be between 5 and 350 characters")
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9A-Za-z\\s-]{4,12}$", message = "Please enter a valid postal code")
    private String postalCode;

    private Boolean isDefault;
}
