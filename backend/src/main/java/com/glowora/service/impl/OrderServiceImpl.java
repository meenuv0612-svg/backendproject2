package com.glowora.service.impl;

import com.glowora.dto.OrderItemRequestDto;
import com.glowora.dto.OrderRequestDto;
import com.glowora.dto.OrderResponseDto;
import com.glowora.dto.OrderStatusUpdateDto;
import com.glowora.entity.*;
import com.glowora.exception.BadRequestException;
import com.glowora.exception.InsufficientStockException;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.*;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto requestDto) {
        // 1. Validate payment method: Only CASH_ON_DELIVERY is functional until a real payment gateway is integrated
        if (requestDto.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            throw new BadRequestException("Payment method '" + requestDto.getPaymentMethod() +
                    "' is a placeholder and unavailable until a live payment gateway is integrated. Please select CASH_ON_DELIVERY.");
        }

        // 2. Resolve customer identity securely
        Long authCustomerId = ownershipValidator.getAuthenticatedCustomerIdOrNull();
        if (authCustomerId != null && requestDto.getCustomerId() != null) {
            ownershipValidator.validateOwnership(requestDto.getCustomerId());
        }
        Long resolvedCustomerId = authCustomerId != null ? authCustomerId : requestDto.getCustomerId();
        if (resolvedCustomerId == null) {
            throw new BadRequestException("Customer ID is required to place an order. Please log in or provide a valid customer ID.");
        }

        Customer customer = customerRepository.findById(resolvedCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + resolvedCustomerId));

        // 3. Determine order items from customer's PostgreSQL cart or explicit request items
        Cart customerCart = cartRepository.findByCustomerId(customer.getId()).orElse(null);
        List<OrderItemRequestDto> requestedItems = new ArrayList<>();

        if (requestDto.getItems() != null && !requestDto.getItems().isEmpty()) {
            requestedItems.addAll(requestDto.getItems());
        } else if (customerCart != null && customerCart.getItems() != null && !customerCart.getItems().isEmpty()) {
            for (CartItem ci : customerCart.getItems()) {
                requestedItems.add(OrderItemRequestDto.builder()
                        .productId(ci.getProduct().getId())
                        .quantity(ci.getQuantity())
                        .build());
            }
        }

        if (requestedItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty. Please add products before placing an order.");
        }

        // 4. Lock products, validate stock and quantities, deduct inventory, and calculate totals on the backend
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .customerName(requestDto.getCustomerName().trim())
                .customerEmail(requestDto.getEmail().trim().toLowerCase())
                .customerPhone(requestDto.getPhone().trim())
                .deliveryAddress(requestDto.getDeliveryAddress().trim())
                .city(requestDto.getCity().trim())
                .state(requestDto.getState().trim())
                .postalCode(requestDto.getPostalCode().trim())
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                // Cash on Delivery orders are never automatically marked as PAID
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.CONFIRMED)
                .orderNotes(requestDto.getOrderNotes())
                .build();

        for (OrderItemRequestDto itemReq : requestedItems) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Order item quantity must be at least 1.");
            }

            // Pessimistic write lock prevents concurrent overselling
            Product product = productRepository.findByIdForUpdate(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer active.");
            }

            int requestedQty = itemReq.getQuantity();
            if (product.getStockQuantity() < requestedQty) {
                throw new InsufficientStockException("Insufficient stock for '" + product.getName() +
                        "'. Available: " + product.getStockQuantity() + ", requested: " + requestedQty + ".");
            }

            // Deduct stock in PostgreSQL
            product.setStockQuantity(product.getStockQuantity() - requestedQty);
            productRepository.save(product);

            // Always use backend PostgreSQL prices, never frontend prices
            BigDecimal unitPrice = product.getPrice();
            BigDecimal origPrice = product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(unitPrice) >= 0
                    ? product.getOriginalPrice() : unitPrice;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            BigDecimal lineOrigTotal = origPrice.multiply(BigDecimal.valueOf(requestedQty));

            subtotal = subtotal.add(lineTotal);
            originalSubtotal = originalSubtotal.add(lineOrigTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productNameSnapshot(product.getName())
                    .productBrandSnapshot(product.getBrand())
                    .productImageSnapshot(product.getImageUrl())
                    .originalPriceSnapshot(origPrice)
                    .unitPriceSnapshot(unitPrice)
                    .quantity(requestedQty)
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);
        }

        BigDecimal discountAmount = originalSubtotal.subtract(subtotal);
        BigDecimal shippingAmount = subtotal.compareTo(BigDecimal.valueOf(999)) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(79);
        BigDecimal finalTotal = subtotal.add(shippingAmount);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setShippingAmount(shippingAmount);
        order.setFinalTotal(finalTotal);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 5. Clear customer's cart after successful order placement
        if (customerCart != null && !customerCart.getItems().isEmpty()) {
            customerCart.getItems().clear();
            cartItemRepository.deleteAllByCartId(customerCart.getId());
        }

        return entityMapper.toOrderDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(entityMapper::toOrderDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        if (order.getCustomer() != null) {
            ownershipValidator.validateOwnership(order.getCustomer().getId());
        }
        return entityMapper.toOrderDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getCustomerOrders(Long customerId) {
        ownershipValidator.validateOwnership(customerId);
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(entityMapper::toOrderDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateDto updateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getCustomer() != null) {
            ownershipValidator.validateOwnership(order.getCustomer().getId());
        }

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = updateDto.getOrderStatus();

        if (currentStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change the status of an already cancelled order.");
        }

        if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
            if (currentStatus == OrderStatus.DELIVERED) {
                throw new BadRequestException("Delivered orders cannot be cancelled.");
            }
            // Restore reserved inventory transactionally
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                            .orElse(null);
                    if (product != null) {
                        product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }

        if (newStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                && updateDto.getPaymentStatus() == null) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (updateDto.getPaymentStatus() != null) {
            order.setPaymentStatus(updateDto.getPaymentStatus());
        }

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);
        return entityMapper.toOrderDto(saved);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "GLW-" + datePart + "-" + randomPart;
    }
}
