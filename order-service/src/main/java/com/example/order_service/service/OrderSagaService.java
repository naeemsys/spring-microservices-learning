
package com.example.order_service.service;

import com.example.order_service.dto.*;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.entity.SagaStatus;
import com.example.order_service.metrics.OrderMetrics;
import com.example.order_service.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OrderSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);

    private final OrderRepository orderRepo;
    private final RabbitTemplate rabbit;
    private final OrderMetrics orderMetrics;

    public OrderSagaService(OrderRepository orderRepo, RabbitTemplate rabbit, OrderMetrics orderMetrics) {
        this.orderRepo = orderRepo;
        this.rabbit = rabbit;
        this.orderMetrics = orderMetrics;
    }

    public Order startSaga(OrderRequest req) {
        // Log entry with business keys (NOT PII)
        log.info("saga=start action=begin_order userId={} qty={}", req.getUserId(), req.getQty());

        Order order = new Order();
        order.setUserId(req.getUserId());
        order.setQty(req.getQty());
        order.setStatus(OrderStatus.CREATED);
        order.setSagaStatus(SagaStatus.STARTED);
        Optional<String> opt = Optional.ofNullable("test");
        opt.map(String::length).orElse(0);

        orderRepo.save(order);
        orderMetrics.increment();

        log.info("saga=start action=order_persisted orderId={} userId={} qty={} status={} sagaStatus={}",
                order.getId(), order.getUserId(), order.getQty(), order.getStatus(), order.getSagaStatus());

        try {
            rabbit.convertAndSend(
                    "saga-exchange",
                    "order.created",
                    new OrderCreatedEvent(order.getId(), req.getUserId(), req.getQty()),
                    message -> {
                            log.info("OUT headers={}", message.getMessageProperties().getHeaders());
                            return message;
                        }
            );


            log.info("saga=start action=event_published exchange={} routingKey={} orderId={}",
                    "saga-exchange", "order.created", order.getId());
        } catch (Exception e) {
            // If AMQP publish fails, compensate immediately
            log.error("saga=start action=event_publish_failed orderId={} reason={}", order.getId(), e.toString(), e);
            compensate(order);
        }

        return order;
    }

    @RabbitListener(queues = "order-queue")
    public void handleUserValidated(UserValidatedEvent event) {
        log.info("saga=continue action=user_validated_event_received orderId={} valid={}", event.orderId(), event.valid());

        Order order = orderRepo.findById(event.orderId()).orElseThrow(() -> {
            log.error("saga=continue action=order_not_found orderId={}", event.orderId());
            return new IllegalStateException("Order not found: " + event.orderId());
        });

        if (!event.valid()) {
            log.warn("saga=continue action=user_validation_failed orderId={}", order.getId());
            compensate(order);
            return;
        }

        order.setSagaStatus(SagaStatus.USER_VALIDATED);
        orderRepo.save(order);
        log.info("saga=continue action=order_updated orderId={} sagaStatus={}", order.getId(), order.getSagaStatus());

        try {
            rabbit.convertAndSend(
                    "saga-exchange",
                    "payment.process",
                    new PaymentProcessedEvent(order.getId(), true)
            );
            log.info("saga=continue action=event_published exchange={} routingKey={} orderId={}",
                    "saga-exchange", "payment.process", order.getId());
        } catch (Exception e) {
            log.error("saga=continue action=payment_event_publish_failed orderId={} reason={}", order.getId(), e.toString(), e);
            compensate(order);
        }
    }

    @RabbitListener(queues = "order-payment-queue")
    public void handlePayment(PaymentProcessedEvent event) {
        log.info("saga=continue action=payment_event_received orderId={} success={}", event.orderId(), event.success());

        Order order = orderRepo.findById(event.orderId()).orElseThrow(() -> {
            log.error("saga=continue action=order_not_found orderId={}", event.orderId());
            return new IllegalStateException("Order not found: " + event.orderId());
        });

        if (!event.success()) {
            log.warn("saga=continue action=payment_failed orderId={}", order.getId());
            compensate(order);
            return;
        }

        order.setSagaStatus(SagaStatus.COMPLETED);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);
        log.info("saga=complete action=order_confirmed orderId={} status={} sagaStatus={}",
                order.getId(), order.getStatus(), order.getSagaStatus());
    }

    private void compensate(Order order) {
        log.info("saga=compensate action=start orderId={}", order.getId());

        order.setSagaStatus(SagaStatus.COMPENSATING);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);
        log.info("saga=compensate action=order_cancelled orderId={} status={} sagaStatus={}",
                order.getId(), order.getStatus(), order.getSagaStatus());

        try {
            rabbit.convertAndSend(
                    "saga-exchange",
                    "payment.refund",
                    new RefundEvent(order.getId())
            );
            log.info("saga=compensate action=refund_event_published exchange={} routingKey={} orderId={}",
                    "saga-exchange", "payment.refund", order.getId());
        } catch (Exception e) {
            log.error("saga=compensate action=refund_event_publish_failed orderId={} reason={}",
                    order.getId(), e.toString(), e);
        }

        order.setSagaStatus(SagaStatus.FAILED);
        orderRepo.save(order);
        log.info("saga=compensate action=finalized orderId={} sagaStatus={}", order.getId(), order.getSagaStatus());
    }
}
