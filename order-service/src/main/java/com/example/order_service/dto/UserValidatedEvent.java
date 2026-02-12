package com.example.order_service.dto;

public record UserValidatedEvent(
        Long orderId,
        boolean valid
) {}
