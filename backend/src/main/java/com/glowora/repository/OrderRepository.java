package com.glowora.repository;

import com.glowora.entity.Order;
import com.glowora.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findTop10ByOrderByCreatedAtDesc();
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    long countByCustomerId(Long customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByOrderStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.finalTotal), 0) FROM Order o WHERE o.orderStatus <> 'CANCELLED'")
    BigDecimal calculateTotalRevenueExcludingCancelled();

    @Query("SELECT COALESCE(SUM(o.finalTotal), 0) FROM Order o WHERE o.orderStatus NOT IN ('PENDING', 'CANCELLED')")
    BigDecimal calculateTotalCompletedRevenue();
}

