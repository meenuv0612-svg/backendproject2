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
public class WishlistItemResponseDto {
    private Long id;
    private Long productId;
    private ProductResponseDto product;
    private LocalDateTime addedAt;
}
