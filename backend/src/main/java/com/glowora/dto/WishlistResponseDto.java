package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponseDto {
    private Long id;
    private Long customerId;
    private int totalItems;
    private List<WishlistItemResponseDto> items;
}
