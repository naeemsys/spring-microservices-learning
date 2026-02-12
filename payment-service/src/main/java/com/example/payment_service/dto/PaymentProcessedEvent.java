package com.example.payment_service.dto;

public record PaymentProcessedEvent(
        Long orderId,
        boolean success
) {}
