package com.glowora.service;

import com.glowora.dto.*;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    PagedResponseDto<ProductResponseDto> getProducts(
            String keyword,
            Long categoryId,
            String parentGroup,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            String sortBy,
            String direction,
            int page,
            int size,
            Boolean activeOnly
    );

    ProductResponseDto getProductById(Long id);
    List<ProductResponseDto> searchProducts(String keyword);
    List<ProductResponseDto> getProductsByCategory(Long categoryId);
    ProductResponseDto createProduct(ProductRequestDto requestDto);
    ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto);
    void deleteProduct(Long id);

    List<ReviewResponseDto> getProductReviews(Long productId);
    ReviewResponseDto addProductReview(Long productId, ReviewRequestDto requestDto);
}
