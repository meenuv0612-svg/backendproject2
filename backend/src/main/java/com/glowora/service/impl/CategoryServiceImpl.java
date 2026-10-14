package com.glowora.service.impl;

import com.glowora.dto.CategoryRequestDto;
import com.glowora.dto.CategoryResponseDto;
import com.glowora.entity.Category;
import com.glowora.exception.BadRequestException;
import com.glowora.exception.DuplicateResourceException;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CategoryRepository;
import com.glowora.repository.ProductRepository;
import com.glowora.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EntityMapper entityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(cat -> {
                    long count = productRepository.countByCategoryOrGroup(cat.getId(), cat.getName());
                    return entityMapper.toCategoryDto(cat, count);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = findCategoryOrThrow(id);
        long count = productRepository.countByCategoryOrGroup(category.getId(), category.getName());
        return entityMapper.toCategoryDto(category, count);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        String name = requestDto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category with name '" + name + "' already exists.");
        }

        Category category = Category.builder()
                .name(name)
                .slug(Category.toSlug(name))
                .parentGroup(requestDto.getParentGroup() != null && !requestDto.getParentGroup().isBlank()
                        ? requestDto.getParentGroup().trim() : name)
                .description(requestDto.getDescription())
                .imageUrl(requestDto.getImageUrl() != null && !requestDto.getImageUrl().isBlank()
                        ? requestDto.getImageUrl().trim() : "assets/images/categories/skincare.svg")
                .active(requestDto.getActive() != null ? requestDto.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        return entityMapper.toCategoryDto(saved, 0L);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto) {
        Category category = findCategoryOrThrow(id);
        String newName = requestDto.getName().trim();

        categoryRepository.findByNameIgnoreCase(newName).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Another category with name '" + newName + "' already exists.");
            }
        });

        category.setName(newName);
        category.setSlug(Category.toSlug(newName));
        if (requestDto.getParentGroup() != null && !requestDto.getParentGroup().isBlank()) {
            category.setParentGroup(requestDto.getParentGroup().trim());
        }
        if (requestDto.getDescription() != null) {
            category.setDescription(requestDto.getDescription());
        }
        if (requestDto.getImageUrl() != null && !requestDto.getImageUrl().isBlank()) {
            category.setImageUrl(requestDto.getImageUrl().trim());
        }
        if (requestDto.getActive() != null) {
            category.setActive(requestDto.getActive());
        }

        Category saved = categoryRepository.save(category);
        long count = productRepository.countByCategoryOrGroup(saved.getId(), saved.getName());
        return entityMapper.toCategoryDto(saved, count);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BadRequestException("Cannot delete category '" + category.getName() +
                    "' because it contains " + productCount + " associated product(s).");
        }
        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }
}
