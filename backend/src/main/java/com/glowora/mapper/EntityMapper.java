package com.glowora.mapper;

import com.glowora.dto.*;
import com.glowora.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class EntityMapper {

    public CustomerResponseDto toCustomerDto(Customer customer) {
        if (customer == null) return null;
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .role(customer.getRole())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public AddressResponseDto toAddressDto(CustomerAddress address) {
        if (address == null) return null;
        return AddressResponseDto.builder()
                .id(address.getId())
                .customerId(address.getCustomer() != null ? address.getCustomer().getId() : null)
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }

    public CategoryResponseDto toCategoryDto(Category category, long productCount) {
        if (category == null) return null;
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentGroup(category.getParentGroup())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.getActive())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public ProductResponseDto toProductDto(Product product) {
        if (product == null) return null;
        Category cat = product.getCategory();
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(cat != null ? cat.getId() : null)
                .categoryName(cat != null ? cat.getName() : null)
                .parentGroup(cat != null ? cat.getParentGroup() : null)
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercentage(product.getDiscountPercentage())
                .stockQuantity(stock)
                .inStock(stock > 0 && Boolean.TRUE.equals(product.getActive()))
                .imageUrl(product.getImageUrl())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .ingredients(product.getIngredients())
                .howToUse(product.getHowToUse())
                .volumeOrWeight(product.getVolumeOrWeight())
                .active(product.getActive())
                .featured(product.getFeatured())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ReviewResponseDto toReviewDto(ProductReview review) {
        if (review == null) return null;
        return ReviewResponseDto.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .customerId(review.getCustomer() != null ? review.getCustomer().getId() : null)
                .customerName(review.getCustomerName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public WishlistResponseDto toWishlistDto(Wishlist wishlist) {
        if (wishlist == null) return null;
        List<WishlistItemResponseDto> itemDtos = new ArrayList<>();
        if (wishlist.getItems() != null) {
            for (WishlistItem item : wishlist.getItems()) {
                itemDtos.add(WishlistItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .product(toProductDto(item.getProduct()))
                        .addedAt(item.getAddedAt())
                        .build());
            }
        }
        return WishlistResponseDto.builder()
                .id(wishlist.getId())
                .customerId(wishlist.getCustomer() != null ? wishlist.getCustomer().getId() : null)
                .totalItems(itemDtos.size())
                .items(itemDtos)
                .build();
    }

    public CartResponseDto toCartDto(Cart cart) {
        if (cart == null) return null;
        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Product p = item.getProduct();
                if (p == null) continue;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal unitPrice = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                BigDecimal origPrice = p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(unitPrice) >= 0
                        ? p.getOriginalPrice() : unitPrice;
                BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal lineOriginalTotal = origPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal lineDiscount = lineOriginalTotal.subtract(lineSubtotal);

                originalSubtotal = originalSubtotal.add(lineOriginalTotal);
                subtotal = subtotal.add(lineSubtotal);
                totalQuantity += qty;

                int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
                itemDtos.add(CartItemResponseDto.builder()
                        .id(item.getId())
                        .productId(p.getId())
                        .productName(p.getName())
                        .brand(p.getBrand())
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                        .imageUrl(p.getImageUrl())
                        .price(unitPrice)
                        .originalPrice(origPrice)
                        .discountPercentage(p.getDiscountPercentage())
                        .stockQuantity(stock)
                        .inStock(stock >= qty && Boolean.TRUE.equals(p.getActive()))
                        .quantity(qty)
                        .lineSubtotal(lineSubtotal)
                        .lineOriginalTotal(lineOriginalTotal)
                        .lineDiscount(lineDiscount)
                        .build());
            }
        }

        BigDecimal discountAmount = originalSubtotal.subtract(subtotal);
        BigDecimal shippingAmount = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.valueOf(999)) < 0) {
            shippingAmount = BigDecimal.valueOf(79);
        }
        BigDecimal finalTotal = subtotal.add(shippingAmount);

        return CartResponseDto.builder()
                .id(cart.getId())
                .customerId(cart.getCustomer() != null ? cart.getCustomer().getId() : null)
                .totalItems(itemDtos.size())
                .totalQuantity(totalQuantity)
                .originalSubtotal(originalSubtotal)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingAmount(shippingAmount)
                .finalTotal(finalTotal)
                .items(itemDtos)
                .build();
    }

    public OrderResponseDto toOrderDto(Order order) {
        if (order == null) return null;
        List<OrderItemResponseDto> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itemDtos.add(OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductNameSnapshot())
                        .productBrand(item.getProductBrandSnapshot())
                        .productImage(item.getProductImageSnapshot())
                        .originalPrice(item.getOriginalPriceSnapshot())
                        .unitPrice(item.getUnitPriceSnapshot())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build());
            }
        }
        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .city(order.getCity())
                .state(order.getState())
                .postalCode(order.getPostalCode())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingAmount(order.getShippingAmount())
                .finalTotal(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .orderNotes(order.getOrderNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemDtos)
                .build();
    }

    public ContactResponseDto toContactDto(ContactMessage msg) {
        if (msg == null) return null;
        return ContactResponseDto.builder()
                .id(msg.getId())
                .fullName(msg.getFullName())
                .email(msg.getEmail())
                .phone(msg.getPhone())
                .subject(msg.getSubject())
                .message(msg.getMessage())
                .status(msg.getStatus())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
