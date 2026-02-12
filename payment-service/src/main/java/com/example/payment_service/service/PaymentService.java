
package com.example.payment_service.service;

import com.example.payment_service.dto.PaymentProcessedEvent;
import com.example.payment_service.dto.RefundEvent;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repo;
    private final RabbitTemplate rabbit;

    public PaymentService(PaymentRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    public Payment create(Payment p) {
        Payment saved = repo.save(p);
        log.info("saga=payment action=payment_persisted paymentId={} orderId={} amount={} status={}",
                saved.getId(), saved.getOrderId(), saved.getAmount(), saved.getStatus());
        return saved;
    }

    public Optional<Payment> findById(Long id) {
        log.debug("saga=payment action=payment_lookup id={}", id);
        return repo.findById(id);
    }

    public void delete(Long id) {
        repo.deleteById(id);
        log.info("saga=payment action=payment_deleted id={}", id);
    }

    @RabbitListener(queues = "payment-queue")
    public void processPayment(PaymentProcessedEvent event) {
        log.info("saga=payment action=payment_event_received orderId={} requestedSuccess={}", event.orderId(), event.success());

        boolean success = executePayment(event); // simulate gateway

        try {
            rabbit.convertAndSend(
                    "saga-exchange",
                    "payment.completed",
                    new PaymentProcessedEvent(event.orderId(), success)
            );
            log.info("saga=payment action=event_published exchange={} routingKey={} orderId={} success={}",
                    "saga-exchange", "payment.completed", event.orderId(), success);
        } catch (Exception e) {
            log.error("saga=payment action=event_publish_failed exchange={} routingKey={} orderId={} reason={}",
                    "saga-exchange", "payment.completed", event.orderId(), e.toString(), e);
        }
    }

    @RabbitListener(queues = "refund-queue")
    public void refund(RefundEvent event) {
        log.info("saga=payment action=refund_event_received orderId={}", event.orderId());
        // Here you'd update the payment record if present, mark refunded, etc.
        log.info("saga=payment action=refund_processed orderId={}", event.orderId());
    }

    /** Simulates a gateway charge and persists the payment. */
    public boolean executePayment(PaymentProcessedEvent event) {
        log.info("saga=payment action=execute_payment orderId={}", event.orderId());
        try {
            Payment p = new Payment();
            // TODO: fetch real amount from order-service by orderId
            p.setAmount(0.0);
            p.setStatus(PaymentStatus.SUCCESS);
            p.setOrderId(event.orderId()); // FIXED: was getOrderId(...)

            create(p);
            log.info("saga=payment action=execute_payment_completed orderId={} status={}", event.orderId(), p.getStatus());
            return true;
        } catch (Exception e) {
            log.error("saga=payment action=execute_payment_failed orderId={} reason={}", event.orderId(), e.toString(), e);
            return false;
        }
    }
}
