package com.glowora.service;

import com.glowora.dto.WishlistItemRequestDto;
import com.glowora.dto.WishlistResponseDto;

public interface WishlistService {
    WishlistResponseDto getWishlist(Long customerId);
    WishlistResponseDto addItemToWishlist(Long customerId, WishlistItemRequestDto requestDto);
    WishlistResponseDto removeItemFromWishlist(Long customerId, Long productId);
}
