package com.glowora.service.impl;

import com.glowora.dto.DashboardSummaryDto;
import com.glowora.dto.OrderResponseDto;
import com.glowora.dto.ProductResponseDto;
import com.glowora.entity.OrderStatus;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.*;
import com.glowora.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final EntityMapper entityMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByActiveTrue();
        long lowStockProducts = productRepository.countByStockQuantityLessThanEqual(5);
        long totalCategories = categoryRepository.count();
        long totalCustomers = customerRepository.countByRole("ROLE_CUSTOMER");
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.calculateTotalCompletedRevenue();
        long totalContacts = contactMessageRepository.count();

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByOrderStatus(status));
        }

        List<ProductResponseDto> topRated = productRepository
                .findTop6ByActiveTrueOrderByRatingDescReviewCountDesc()
                .stream()
                .map(entityMapper::toProductDto)
                .toList();

        List<OrderResponseDto> recentOrders = orderRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(entityMapper::toOrderDto)
                .toList();

        return DashboardSummaryDto.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStockProducts)
                .totalCategories(totalCategories)
                .totalCustomers(totalCustomers)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueCalculationRule("Sum of finalTotal for orders with status CONFIRMED, PACKED, SHIPPED, or DELIVERED (PENDING and CANCELLED orders are excluded)")
                .totalContactMessages(totalContacts)
                .ordersByStatus(ordersByStatus)
                .topRatedProducts(topRated)
                .recentOrders(recentOrders)
                .build();
    }
}

