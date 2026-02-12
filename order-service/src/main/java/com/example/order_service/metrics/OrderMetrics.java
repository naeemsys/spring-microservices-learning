package com.example.order_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter orderCreatedCounter;

    public OrderMetrics(MeterRegistry registry) {
        this.orderCreatedCounter =
            Counter.builder("orders.created.total")
                   .description("Total orders created")
                   .register(registry);
    }

    public void increment() {
        orderCreatedCounter.increment();
    }
}
