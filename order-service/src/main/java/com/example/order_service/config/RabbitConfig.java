package com.example.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.*;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange("saga-exchange");
    }

    @Bean
    public Queue orderQueue() {
        return new Queue("order-queue");
    }

    @Bean
    public Queue orderPaymentQueue() {
        return new Queue("order-payment-queue");
    }

    // Bind order queue to "user.validated"
    @Bean
    public Binding orderQueueBinding(Queue orderQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderQueue).to(sagaExchange).with("user.validated");
    }

    // Bind order-payment queue to "payment.completed"
    @Bean
    public Binding orderPaymentQueueBinding(Queue orderPaymentQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderPaymentQueue).to(sagaExchange).with("payment.completed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    ContainerCustomizer<SimpleMessageListenerContainer> rabbitListenerObservation() {
        return container -> container.setObservationEnabled(true); // critical
    }

}
