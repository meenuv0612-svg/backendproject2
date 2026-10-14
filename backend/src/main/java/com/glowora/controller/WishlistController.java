package com.glowora.controller;

import com.glowora.dto.WishlistItemRequestDto;
import com.glowora.dto.WishlistResponseDto;
import com.glowora.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping("/{customerId}")
    public ResponseEntity<WishlistResponseDto> getWishlist(@PathVariable Long customerId) {
        return ResponseEntity.ok(wishlistService.getWishlist(customerId));
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<WishlistResponseDto> addItemToWishlist(@PathVariable Long customerId,
                                                                 @Valid @RequestBody WishlistItemRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addItemToWishlist(customerId, requestDto));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<WishlistResponseDto> removeItemFromWishlist(@PathVariable Long customerId,
                                                                      @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeItemFromWishlist(customerId, productId));
    }
}
