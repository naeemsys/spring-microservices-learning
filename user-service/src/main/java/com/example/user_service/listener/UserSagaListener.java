
package com.example.user_service.listener;

import com.example.user_service.dto.OrderCreatedEvent;
import com.example.user_service.dto.UserValidatedEvent;
import com.example.user_service.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserSagaListener {

    private static final Logger log = LoggerFactory.getLogger(UserSagaListener.class);

    private final UserRepository repo;
    private final RabbitTemplate rabbit;

    public UserSagaListener(UserRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = "user-queue")
    public void validateUser(Message message, OrderCreatedEvent event) {

        log.info("IN headers={}", message.getMessageProperties().getHeaders());
        log.info("traceId={}", MDC.get("traceId"));

        log.info("saga=user action=order_created_event_received orderId={} userId={} amount={}",
                event.orderId(), event.userId(), event.amount());

        boolean exists = false;
        try {
            exists = repo.existsById(event.userId());
            log.info("saga=user action=user_lookup_completed userId={} exists={}", event.userId(), exists);
        } catch (Exception e) {
            log.error("saga=user action=user_lookup_failed userId={} reason={}", event.userId(), e.toString(), e);
        }

        try {
            rabbit.convertAndSend(
                    "saga-exchange",
                    "user.validated",
                    new UserValidatedEvent(event.orderId(), exists)
            );
            log.info("saga=user action=event_published exchange={} routingKey={} orderId={} valid={}",
                    "saga-exchange", "user.validated", event.orderId(), exists);
        } catch (Exception e) {
            log.error("saga=user action=event_publish_failed exchange={} routingKey={} orderId={} reason={}",
                    "saga-exchange", "user.validated", event.orderId(), e.toString(), e);
        }
    }
}
