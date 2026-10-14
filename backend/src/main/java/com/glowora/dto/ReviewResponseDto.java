package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
    private Long id;
    private Long productId;
    private Long customerId;
    private String customerName;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean verifiedPurchase;
    private LocalDateTime createdAt;
}
