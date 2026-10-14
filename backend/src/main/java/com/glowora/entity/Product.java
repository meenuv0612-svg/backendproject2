package com.glowora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "discount_percentage", nullable = false)
    @Builder.Default
    private Integer discountPercentage = 0;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "image_url", nullable = false, length = 350)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 4.5;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(length = 1000)
    private String ingredients;

    @Column(name = "how_to_use", length = 1000)
    private String howToUse;

    @Column(name = "volume_or_weight", length = 60)
    private String volumeOrWeight;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductReview> reviews = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizePricingAndDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizePricingAndDefaults();
    }

    public void normalizePricingAndDefaults() {
        if (this.active == null) this.active = true;
        if (this.featured == null) this.featured = false;
        if (this.rating == null) this.rating = 4.5;
        if (this.reviewCount == null) this.reviewCount = 0;
        if (this.stockQuantity == null) this.stockQuantity = 0;
        if (this.originalPrice == null || this.originalPrice.compareTo(this.price) < 0) {
            this.originalPrice = this.price;
        }
        if (this.originalPrice != null && this.price != null && this.originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (this.originalPrice.compareTo(this.price) > 0) {
                BigDecimal diff = this.originalPrice.subtract(this.price);
                this.discountPercentage = diff.multiply(BigDecimal.valueOf(100))
                        .divide(this.originalPrice, 0, RoundingMode.HALF_UP)
                        .intValue();
            } else {
                this.discountPercentage = 0;
            }
        } else if (this.discountPercentage == null) {
            this.discountPercentage = 0;
        }
    }
}
