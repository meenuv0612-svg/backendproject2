package com.glowora.service.impl;

import com.glowora.dto.WishlistItemRequestDto;
import com.glowora.dto.WishlistResponseDto;
import com.glowora.entity.Customer;
import com.glowora.entity.Product;
import com.glowora.entity.Wishlist;
import com.glowora.entity.WishlistItem;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.ProductRepository;
import com.glowora.repository.WishlistItemRepository;
import com.glowora.repository.WishlistRepository;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public WishlistResponseDto getWishlist(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        Wishlist wishlist = getOrCreateWishlist(customerId);
        return entityMapper.toWishlistDto(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponseDto addItemToWishlist(Long customerId, WishlistItemRequestDto requestDto) {
        ownershipValidator.validateOwnership(customerId);
        Wishlist wishlist = getOrCreateWishlist(customerId);
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + requestDto.getProductId()));

        Optional<WishlistItem> existing = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), product.getId());

        if (existing.isEmpty()) {
            WishlistItem item = WishlistItem.builder()
                    .wishlist(wishlist)
                    .product(product)
                    .build();
            wishlist.getItems().add(item);
            wishlistRepository.save(wishlist);
        }

        return entityMapper.toWishlistDto(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponseDto removeItemFromWishlist(Long customerId, Long productId) {
        ownershipValidator.validateOwnership(customerId);
        Wishlist wishlist = getOrCreateWishlist(customerId);

        WishlistItem item = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + productId + " is not in the wishlist."));

        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);
        return entityMapper.toWishlistDto(wishlist);
    }

    private Wishlist getOrCreateWishlist(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        return wishlistRepository.findByCustomerId(customerId)
                .orElseGet(() -> wishlistRepository.save(Wishlist.builder().customer(customer).build()));
    }
}
