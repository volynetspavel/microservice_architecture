package com.microservice.resource.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that ResourceUploadedEventPublisher delivers a message
 * to the RabbitMQ "resource-uploaded" exchange with the correct JSON payload.
 *
 * HOW TO RUN:
 *   mvn test -pl resource-service -Dtest=ResourceUploadedEventPublisherIT
 *   Requires Docker to be running on the host machine.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // dummy S3 values — S3Client construction does not open a connection
        "aws.s3.endpoint=http://localhost:4566",
        "aws.s3.access-key=test",
        "aws.s3.secret-key=test",
        "aws.s3.bucket-name=test-bucket"
})
@DirtiesContext
class ResourceUploadedEventPublisherIT {

    private static final String EXCHANGE = "resource-uploaded";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");

    @Autowired
    private ResourceUploadedEventPublisher publisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    private String testQueueName;

    @BeforeEach
    void bindTestQueue() {
        // Declare the exchange explicitly — Spring Cloud Stream creates it lazily on first send,
        // so it may not exist yet when we try to bind a spy queue to it
        amqpAdmin.declareExchange(new TopicExchange(EXCHANGE, true, false));

        // Create a temporary non-auto-delete queue bound to the exchange
        // autoDelete=false is intentional: receive() uses basicConsume/basicCancel internally,
        // which would trigger auto-deletion after the last message is consumed
        testQueueName = "test-spy-" + UUID.randomUUID();
        Queue queue = new Queue(testQueueName, true, false, false);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(new Binding(testQueueName, Binding.DestinationType.QUEUE,
                EXCHANGE, "#", null));

        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    @AfterEach
    void deleteTestQueue() {
        amqpAdmin.deleteQueue(testQueueName);
    }

    @Test
    void publish_validResourceId_messageDeliveredToExchange() {
        publisher.publish(42);

        Message received = rabbitTemplate.receive(testQueueName, 5_000);

        assertThat(received).isNotNull();
    }

    @Test
    void publish_validResourceId_payloadContainsResourceId() throws Exception {
        publisher.publish(7);

        Message received = rabbitTemplate.receive(testQueueName, 5_000);

        assertThat(received).isNotNull();
        String body = new String(received.getBody());
        assertThat(body).contains("7");
    }

    @Test
    void publish_multipleEvents_allDelivered() {
        publisher.publish(1);
        publisher.publish(2);
        publisher.publish(3);

        int count = 0;
        while (rabbitTemplate.receive(testQueueName, 2_000) != null) {
            count++;
        }

        assertThat(count).isEqualTo(3);
    }
}
