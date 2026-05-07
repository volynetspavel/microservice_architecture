package com.microservice.processor.config;

import com.microservice.processor.dto.ResourceUploadedEvent;
import com.microservice.processor.messaging.ResourceUploadedConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Configuration class for messaging components.
 */
@Configuration
public class MessagingConfig {

    /**
     * Defines a Consumer bean that processes ResourceUploadedEvent messages using
     * the ResourceUploadedConsumer implementation.
     *
     * @param consumer The ResourceUploadedConsumer instance that will handle incoming ResourceUploadedEvent messages.
     * @return A Consumer bean that can be used by Spring Cloud Stream to listen for messages from RabbitMQ
     * and delegate processing to the ResourceUploadedConsumer.
     */
    @Bean
    public Consumer<ResourceUploadedEvent> processResourceUploaded(ResourceUploadedConsumer consumer) {
        return consumer;
    }
}
