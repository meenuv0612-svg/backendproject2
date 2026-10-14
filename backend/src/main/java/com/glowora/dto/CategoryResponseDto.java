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
public class CategoryResponseDto {
    private Long id;
    private String name;
    private String slug;
    private String parentGroup;
    private String description;
    private String imageUrl;
    private Boolean active;
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
