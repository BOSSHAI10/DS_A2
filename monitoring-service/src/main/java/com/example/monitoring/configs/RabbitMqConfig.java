package com.example.monitoring.configs;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // Numele cozii trebuie să fie același cu cel definit în Simulator
    @Bean
    public Queue queue() {
        return new Queue("device_queue", true); // "device_queue" e un exemplu, verifică simulatorul
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}