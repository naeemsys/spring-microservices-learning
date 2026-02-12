package com.example.payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

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

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange("saga-exchange");
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue("payment-queue");
    }

    @Bean
    public Queue refundQueue() {
        return new Queue("refund-queue");
    }

    // Bind payment queue to "payment.process"
    @Bean
    public Binding paymentQueueBinding(Queue paymentQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentQueue).to(sagaExchange).with("payment.process");
    }

    // Bind refund queue to "payment.refund"
    @Bean
    public Binding refundQueueBinding(Queue refundQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(refundQueue).to(sagaExchange).with("payment.refund");
    }
}
