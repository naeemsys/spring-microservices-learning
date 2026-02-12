package com.example.user_service.dto;

public record UserValidatedEvent(
        Long orderId,
        boolean valid
) {}
