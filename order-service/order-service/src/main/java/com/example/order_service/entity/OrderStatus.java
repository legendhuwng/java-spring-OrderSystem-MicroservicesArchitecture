package com.example.order_service.entity;

public enum OrderStatus {
    PENDING,
    INVENTORY_CHECKING,
    PAYMENT_PROCESSING,
    SHIPPING,
    COMPLETED,
    CANCELLED
}