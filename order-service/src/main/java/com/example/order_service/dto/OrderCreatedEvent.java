package com.example.order_service.dto;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        double amount
) {}
