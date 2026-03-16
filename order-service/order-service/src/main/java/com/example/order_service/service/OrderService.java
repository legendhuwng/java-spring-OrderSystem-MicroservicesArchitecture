// OrderService.java
package com.example.order_service.service;

import com.example.order_service.dto.*;
import com.example.order_service.entity.*;
import com.example.order_service.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional  // ← Order + Outbox lưu trong cùng 1 transaction
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Build Order entity
        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);

        // 2. Lưu Outbox event trong cùng transaction
        try {
            String payload = objectMapper.writeValueAsString(toResponse(saved));
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(saved.getId())
                    .eventType("order-created")
                    .payload(payload)
                    .build();
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to create outbox event for order {}", saved.getId(), e);
            throw new RuntimeException("Failed to create outbox event", e);
        }

        log.info("Order created: id={}, status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Transactional
public void cancelOrderAndReleaseInventory(String orderId) {
    // 1. Update order status → CANCELLED
    updateOrderStatus(orderId, OrderStatus.CANCELLED);

    // 2. Publish inventory-release-command để Inventory Service rollback stock
    try {
        String payload = "{\"orderId\":\"" + orderId + "\",\"action\":\"RELEASE\"}";
        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .eventType("inventory-release-command")
                .payload(payload)
                .build();
        outboxEventRepository.save(outbox);
        log.info("Queued inventory-release-command for orderId={}", orderId);
    } catch (Exception e) {
        log.error("Failed to queue inventory-release-command for orderId={}", orderId, e);
        throw new RuntimeException("Failed to queue rollback command", e);
    }
}

}