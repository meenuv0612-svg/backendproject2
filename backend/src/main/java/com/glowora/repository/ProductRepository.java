package com.glowora.repository;

import com.glowora.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category.parentGroup) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchActiveProducts(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId OR LOWER(p.category.parentGroup) = LOWER(:categoryName)")
    List<Product> findByCategoryOrGroup(@Param("categoryId") Long categoryId, @Param("categoryName") String categoryName);

    long countByCategoryId(Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId OR LOWER(p.category.parentGroup) = LOWER(:categoryName)")
    long countByCategoryOrGroup(@Param("categoryId") Long categoryId, @Param("categoryName") String categoryName);

    long countByActiveTrue();

    long countByStockQuantityLessThanEqual(Integer threshold);

    List<Product> findTop6ByActiveTrueOrderByRatingDescReviewCountDesc();
}
