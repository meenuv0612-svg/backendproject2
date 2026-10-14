package com.glowora.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 180, message = "Product name must be between 2 and 180 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(min = 5, max = 2000, message = "Description must be between 5 and 2000 characters")
    private String description;

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @Positive(message = "Original price must be greater than zero")
    private BigDecimal originalPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "Image URL or path is required")
    @Size(max = 350, message = "Image URL cannot exceed 350 characters")
    private String imageUrl;

    @DecimalMin(value = "0.0", message = "Rating cannot be less than 0")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5")
    private Double rating;

    @Size(max = 1000, message = "Ingredients cannot exceed 1000 characters")
    private String ingredients;

    @Size(max = 1000, message = "How to use cannot exceed 1000 characters")
    private String howToUse;

    @Size(max = 60, message = "Volume or weight cannot exceed 60 characters")
    private String volumeOrWeight;

    private Boolean active;
    private Boolean featured;
}
