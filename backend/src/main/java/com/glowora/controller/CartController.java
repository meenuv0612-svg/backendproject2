package com.glowora.controller;

import com.glowora.dto.CartItemRequestDto;
import com.glowora.dto.CartItemUpdateDto;
import com.glowora.dto.CartResponseDto;
import com.glowora.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.getCart(customerId));
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartResponseDto> addItemToCart(@PathVariable Long customerId,
                                                         @Valid @RequestBody CartItemRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(customerId, requestDto));
    }

    @PutMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(@PathVariable Long customerId,
                                                                  @PathVariable Long productId,
                                                                  @Valid @RequestBody CartItemUpdateDto updateDto) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(customerId, productId, updateDto));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartResponseDto> removeItemFromCart(@PathVariable Long customerId,
                                                              @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(customerId, productId));
    }

    @DeleteMapping("/{customerId}/clear")
    public ResponseEntity<CartResponseDto> clearCart(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.clearCart(customerId));
    }
}
