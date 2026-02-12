package com.example.user_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
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
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
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
    public Queue userQueue() {
        return new Queue("user-queue");
    }

    // Bind user queue to "order.created"
    @Bean
    public Binding userQueueBinding(Queue userQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(userQueue).to(sagaExchange).with("order.created");
    }

}
