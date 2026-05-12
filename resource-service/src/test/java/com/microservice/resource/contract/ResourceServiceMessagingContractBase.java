package com.microservice.resource.contract;

import com.microservice.resource.service.ResourceUploadedEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for Spring Cloud Contract provider verifier tests for messaging contracts.
 *
 * Uses a minimal context containing only ResourceUploadedEventPublisher (which needs
 * only StreamBridge). Spring Cloud Stream's test binder (spring-cloud-stream-test-binder)
 * auto-configures StreamBridge and @AutoConfigureMessageVerifier wires the in-memory
 * message verifier. No database, no S3, no RabbitMQ broker needed.
 *
 * The generated test (MessagingTest) calls publishResourceUploadedEvent(), matching
 * triggeredBy() in shouldPublishResourceUploadedEvent.groovy.
 */
@SpringBootTest(
        classes = ResourceServiceMessagingContractBase.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
@TestPropertySource(properties = {
        "spring.cloud.stream.default-binder=test"
})
public abstract class ResourceServiceMessagingContractBase {

    @Configuration
    @Import(ResourceUploadedEventPublisher.class)
    static class TestConfig {
    }

    @Autowired
    ResourceUploadedEventPublisher publisher;

    public void publishResourceUploadedEvent() {
        publisher.publish(1);
    }
}