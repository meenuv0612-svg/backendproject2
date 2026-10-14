package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private String brand;
    private String categoryName;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercentage;
    private Integer stockQuantity;
    private Boolean inStock;
    private Integer quantity;
    private BigDecimal lineSubtotal;
    private BigDecimal lineOriginalTotal;
    private BigDecimal lineDiscount;
}
