package com.glowora.service;

import com.glowora.dto.CartItemRequestDto;
import com.glowora.dto.CartItemUpdateDto;
import com.glowora.dto.CartResponseDto;

public interface CartService {
    CartResponseDto getCart(Long customerId);
    CartResponseDto addItemToCart(Long customerId, CartItemRequestDto requestDto);
    CartResponseDto updateCartItemQuantity(Long customerId, Long productId, CartItemUpdateDto updateDto);
    CartResponseDto removeItemFromCart(Long customerId, Long productId);
    CartResponseDto clearCart(Long customerId);
}
