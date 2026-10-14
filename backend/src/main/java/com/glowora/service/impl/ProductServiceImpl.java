package com.glowora.service.impl;

import com.glowora.dto.*;
import com.glowora.entity.Category;
import com.glowora.entity.Customer;
import com.glowora.entity.Product;
import com.glowora.entity.ProductReview;
import com.glowora.exception.BadRequestException;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CategoryRepository;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.ProductRepository;
import com.glowora.repository.ProductReviewRepository;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final EntityMapper entityMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductResponseDto> getProducts(
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
            Boolean activeOnly) {

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price.");
        }

        String categoryNameForGroupMatch = null;
        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId).orElse(null);
            if (cat != null) {
                categoryNameForGroupMatch = cat.getName();
            }
        }
        final String resolvedGroupName = categoryNameForGroupMatch;

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly == null || Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate namePred = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                Predicate brandPred = cb.like(cb.lower(root.get("brand")), pattern);
                Predicate catNamePred = cb.like(cb.lower(root.get("category").get("name")), pattern);
                Predicate groupPred = cb.like(cb.lower(root.get("category").get("parentGroup")), pattern);
                predicates.add(cb.or(namePred, descPred, brandPred, catNamePred, groupPred));
            }

            if (categoryId != null) {
                if (resolvedGroupName != null) {
                    predicates.add(cb.or(
                            cb.equal(root.get("category").get("id"), categoryId),
                            cb.equal(cb.lower(root.get("category").get("parentGroup")), resolvedGroupName.toLowerCase())
                    ));
                } else {
                    predicates.add(cb.equal(root.get("category").get("id"), categoryId));
                }
            }

            if (parentGroup != null && !parentGroup.trim().isEmpty()) {
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("category").get("parentGroup")), parentGroup.trim().toLowerCase()),
                        cb.equal(cb.lower(root.get("category").get("name")), parentGroup.trim().toLowerCase())
                ));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String sortField = resolveSortField(sortBy);
        Sort.Direction sortDir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 12 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(sortDir, sortField));
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductResponseDto> dtos = productPage.getContent().stream()
                .map(entityMapper::toProductDto)
                .toList();

        return PagedResponseDto.<ProductResponseDto>builder()
                .content(dtos)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return entityMapper.toProductDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                    .stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .map(entityMapper::toProductDto)
                    .toList();
        }
        return productRepository.searchActiveProducts(keyword.trim())
                .stream()
                .map(entityMapper::toProductDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        return productRepository.findByCategoryOrGroup(category.getId(), category.getName())
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(entityMapper::toProductDto)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        validateProductPricingAndStock(requestDto);
        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + requestDto.getCategoryId()));

        BigDecimal originalPrice = requestDto.getOriginalPrice() != null &&
                requestDto.getOriginalPrice().compareTo(requestDto.getPrice()) >= 0
                ? requestDto.getOriginalPrice() : requestDto.getPrice();

        Product product = Product.builder()
                .name(requestDto.getName().trim())
                .description(requestDto.getDescription().trim())
                .brand(requestDto.getBrand().trim())
                .category(category)
                .price(requestDto.getPrice())
                .originalPrice(originalPrice)
                .stockQuantity(requestDto.getStockQuantity())
                .imageUrl(requestDto.getImageUrl().trim())
                .rating(requestDto.getRating() != null ? requestDto.getRating() : 4.6)
                .reviewCount(0)
                .ingredients(requestDto.getIngredients())
                .howToUse(requestDto.getHowToUse())
                .volumeOrWeight(requestDto.getVolumeOrWeight())
                .active(requestDto.getActive() != null ? requestDto.getActive() : true)
                .featured(requestDto.getFeatured() != null ? requestDto.getFeatured() : false)
                .build();
        product.normalizePricingAndDefaults();

        Product saved = productRepository.save(product);
        return entityMapper.toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        validateProductPricingAndStock(requestDto);
        Product product = findProductOrThrow(id);
        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + requestDto.getCategoryId()));

        BigDecimal originalPrice = requestDto.getOriginalPrice() != null &&
                requestDto.getOriginalPrice().compareTo(requestDto.getPrice()) >= 0
                ? requestDto.getOriginalPrice() : requestDto.getPrice();

        product.setName(requestDto.getName().trim());
        product.setDescription(requestDto.getDescription().trim());
        product.setBrand(requestDto.getBrand().trim());
        product.setCategory(category);
        product.setPrice(requestDto.getPrice());
        product.setOriginalPrice(originalPrice);
        product.setStockQuantity(requestDto.getStockQuantity());
        product.setImageUrl(requestDto.getImageUrl().trim());
        if (requestDto.getRating() != null) {
            product.setRating(requestDto.getRating());
        }
        if (requestDto.getIngredients() != null) {
            product.setIngredients(requestDto.getIngredients());
        }
        if (requestDto.getHowToUse() != null) {
            product.setHowToUse(requestDto.getHowToUse());
        }
        if (requestDto.getVolumeOrWeight() != null) {
            product.setVolumeOrWeight(requestDto.getVolumeOrWeight());
        }
        if (requestDto.getActive() != null) {
            product.setActive(requestDto.getActive());
        }
        if (requestDto.getFeatured() != null) {
            product.setFeatured(requestDto.getFeatured());
        }
        product.normalizePricingAndDefaults();

        Product saved = productRepository.save(product);
        return entityMapper.toProductDto(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getProductReviews(Long productId) {
        findProductOrThrow(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(entityMapper::toReviewDto)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponseDto addProductReview(Long productId, ReviewRequestDto requestDto) {
        Product product = findProductOrThrow(productId);

        Customer customer = null;
        Long authCustomerId = ownershipValidator.getAuthenticatedCustomerIdOrNull();
        Long targetCustomerId = authCustomerId != null ? authCustomerId : requestDto.getCustomerId();
        if (targetCustomerId != null) {
            ownershipValidator.validateOwnership(targetCustomerId);
            customer = customerRepository.findById(targetCustomerId).orElse(null);
        }

        ProductReview review = ProductReview.builder()
                .product(product)
                .customer(customer)
                .customerName(requestDto.getCustomerName().trim())
                .rating(requestDto.getRating())
                .title(requestDto.getTitle() != null ? requestDto.getTitle().trim() : "Verified Glowora Review")
                .comment(requestDto.getComment().trim())
                .verifiedPurchase(customer != null)
                .build();

        ProductReview savedReview = reviewRepository.save(review);

        Double avg = reviewRepository.calculateAverageRating(productId);
        long count = reviewRepository.countByProductId(productId);
        if (avg != null) {
            double rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
            product.setRating(rounded);
        }
        product.setReviewCount((int) count);
        productRepository.save(product);

        return entityMapper.toReviewDto(savedReview);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    private void validateProductPricingAndStock(ProductRequestDto dto) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Product price must be greater than zero.");
        }
        if (dto.getStockQuantity() == null || dto.getStockQuantity() < 0) {
            throw new BadRequestException("Product stock quantity cannot be negative.");
        }
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "price" -> "price";
            case "rating" -> "rating";
            case "name" -> "name";
            case "discount", "discountpercentage" -> "discountPercentage";
            case "createdat", "newest" -> "createdAt";
            default -> "id";
        };
    }
}
