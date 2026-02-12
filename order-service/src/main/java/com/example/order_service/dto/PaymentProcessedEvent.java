package com.example.order_service.dto;

public record PaymentProcessedEvent(
        Long orderId,
        boolean success
) {}
