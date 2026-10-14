package com.glowora.repository;

import com.glowora.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    Optional<Category> findBySlugIgnoreCase(String slug);
    boolean existsByNameIgnoreCase(String name);
    List<Category> findByParentGroupIgnoreCase(String parentGroup);
    List<Category> findAllByOrderByIdAsc();
}
