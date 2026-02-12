package com.example.user_service.dto;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        double amount
) {}
