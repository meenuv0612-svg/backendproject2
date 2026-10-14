package com.glowora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 80, message = "Parent group cannot exceed 80 characters")
    private String parentGroup;

    @Size(max = 600, message = "Description cannot exceed 600 characters")
    private String description;

    @Size(max = 350, message = "Image URL cannot exceed 350 characters")
    private String imageUrl;

    private Boolean active;
}
