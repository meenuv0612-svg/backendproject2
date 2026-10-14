package com.glowora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalCategories;
    private long totalCustomers;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private String revenueCalculationRule;
    private long totalContactMessages;
    private Map<String, Long> ordersByStatus;
    private List<ProductResponseDto> topRatedProducts;
    private List<OrderResponseDto> recentOrders;
}

