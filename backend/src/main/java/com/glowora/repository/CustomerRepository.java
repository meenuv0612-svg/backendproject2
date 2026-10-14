package com.glowora.repository;

import com.glowora.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(String role);
    List<Customer> findByRoleOrderByCreatedAtDesc(String role);

    @Query("SELECT c FROM Customer c WHERE c.role = 'ROLE_CUSTOMER' AND " +
           "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<Customer> searchCustomersByKeyword(@Param("keyword") String keyword);
}

