package com.example.order_service.entity;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}