package com.glowora.service;

import com.glowora.dto.OrderRequestDto;
import com.glowora.dto.OrderResponseDto;
import com.glowora.dto.OrderStatusUpdateDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto placeOrder(OrderRequestDto requestDto);
    List<OrderResponseDto> getAllOrders();
    OrderResponseDto getOrderById(Long orderId);
    List<OrderResponseDto> getCustomerOrders(Long customerId);
    OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateDto updateDto);
}
