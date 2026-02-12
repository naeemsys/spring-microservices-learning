
package com.example.order_service.service;

import com.example.order_service.client.UserClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.UserResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.metrics.OrderMetrics;
import com.example.order_service.repo.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final OrderMetrics orderMetrics;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderRepository, UserClient userClient, OrderMetrics orderMetrics){
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.orderMetrics = orderMetrics;
    }

    // Orchestration Saga: An order can only be created if the user exists
    public Order create(OrderRequest request) {
        log.info("Creating order: userId={}, product={}, qty={}", request.getUserId(), request.getProduct(), request.getQty());

        // Step 1: Create order in CREATED state
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProduct(request.getProduct());
        order.setQty(request.getQty());
        order.setStatus(OrderStatus.CREATED);

        orderRepository.save(order);
        log.debug("Order persisted in CREATED state: orderId={}", order.getId());

        orderMetrics.increment();
        log.debug("Order metrics incremented for orderId={}", order.getId());

        try {
            // Step 2: Validate User
            log.info("Validating user for orderId={}, userId={}", order.getId(), request.getUserId());
            validateUser(request.getUserId());
            log.info("User validation succeeded: userId={}", request.getUserId());

            // Step 3: Confirm order
            order.setStatus(OrderStatus.CONFIRMED);
            Order saved = orderRepository.save(order);
            log.info("Order confirmed: orderId={}, status={}", saved.getId(), saved.getStatus());
            return saved;

        } catch (Exception ex) {
            // Step 4: Compensation
            log.warn("Order validation failed, initiating compensation: orderId={}, reason={}", order.getId(), ex.getMessage(), ex);
            cancelOrder(order.getId());
            log.info("Order cancelled via compensation: orderId={}", order.getId());
            throw new RuntimeException("Order failed, user not valid", ex);
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    public void validateUser(Long userId) {
        log.debug("Calling User Service: getUser(userId={})", userId);
        userClient.getUser(userId);
        log.debug("User Service call completed: userId={}", userId);
    }

    public UserResponse userFallback(Long userId, Throwable ex) {
        // This is invoked by Resilience4j on failures/open circuit
        log.error("User service fallback triggered: userId={}, errorType={}, message={}",
                userId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        throw new RuntimeException("User service unavailable", ex);
    }

    public void cancelOrder(Long orderId) {
        log.info("Cancelling order: orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found during cancellation: orderId={}", orderId);
                    return new IllegalArgumentException("Order not found: " + orderId);
                });

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled successfully: orderId={}, status={}", orderId, order.getStatus());
    }


//    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
//    public Order create(OrderRequest req){
//        // call user service to validate user exists
//        var user = userClient.getUser(req.getUserId());
//        // if user is null, Feign will throw; here we assume user exists
//        Order o = new Order();
//        o.setUserId(req.getUserId());
//        o.setProduct(req.getProduct());
//        o.setQty(req.getQty());
//        return repo.save(o);
//    }
//    public Order userFallback(OrderRequest req, Throwable t){
//        // fallback: throw an IllegalStateException for demo (could queue the order)
//        throw new IllegalStateException("User service unavailable; cannot create order now.");
//    }

    public List<Order> list(){ return orderRepository.findAll(); }
}
