package com.glowora.service.impl;

import com.glowora.dto.CartItemRequestDto;
import com.glowora.dto.CartItemUpdateDto;
import com.glowora.dto.CartResponseDto;
import com.glowora.entity.Cart;
import com.glowora.entity.CartItem;
import com.glowora.entity.Customer;
import com.glowora.entity.Product;
import com.glowora.exception.BadRequestException;
import com.glowora.exception.InsufficientStockException;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CartItemRepository;
import com.glowora.repository.CartRepository;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.ProductRepository;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public CartResponseDto getCart(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        Cart cart = getOrCreateCart(customerId);
        return entityMapper.toCartDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto addItemToCart(Long customerId, CartItemRequestDto requestDto) {
        ownershipValidator.validateOwnership(customerId);
        if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + requestDto.getProductId()));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Product '" + product.getName() + "' is currently unavailable.");
        }

        Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        int newQuantity = requestDto.getQuantity() + existingOpt.map(CartItem::getQuantity).orElse(0);

        if (product.getStockQuantity() < newQuantity) {
            throw new InsufficientStockException("Insufficient stock for '" + product.getName() +
                    "'. Available stock: " + product.getStockQuantity() + ", requested in cart: " + newQuantity + ".");
        }

        if (existingOpt.isPresent()) {
            CartItem existingItem = existingOpt.get();
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(newQuantity)
                    .build();
            cart.getItems().add(newItem);
            cartRepository.save(cart);
        }

        return entityMapper.toCartDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto updateCartItemQuantity(Long customerId, Long productId, CartItemUpdateDto updateDto) {
        ownershipValidator.validateOwnership(customerId);
        if (updateDto.getQuantity() == null || updateDto.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCart(customerId);
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + productId + " is not in the cart."));

        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < updateDto.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for '" + product.getName() +
                    "'. Available stock: " + product.getStockQuantity() + ", requested: " + updateDto.getQuantity() + ".");
        }

        cartItem.setQuantity(updateDto.getQuantity());
        cartItemRepository.save(cartItem);
        return entityMapper.toCartDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto removeItemFromCart(Long customerId, Long productId) {
        ownershipValidator.validateOwnership(customerId);
        Cart cart = getOrCreateCart(customerId);

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + productId + " is not in the cart."));

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        return entityMapper.toCartDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto clearCart(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cartItemRepository.deleteAllByCartId(cart.getId());
        return entityMapper.toCartDto(cart);
    }

    private Cart getOrCreateCart(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.builder().customer(customer).build()));
    }
}
