package com.microservice.resource.service;

import com.microservice.resource.dto.ResourceUploadedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

/**
 * Publishes a {@link ResourceUploadedEvent} to RabbitMQ via Spring Cloud Stream.
 * The binding name maps to the "resource-uploaded" exchange declared in application.properties.
 */
@Slf4j
@Service
public class ResourceUploadedEventPublisher {

    // "-out-0" suffix: Spring Cloud Stream convention for an output binding at index 0
    private static final String BINDING_NAME = "resource-uploaded-out-0";
    private final StreamBridge streamBridge;

    public ResourceUploadedEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /**
     * Publishes a ResourceUploadedEvent containing the resourceId to RabbitMQ.
     * @param resourceId The ID of the uploaded resource to include in the event payload.
     */
    public void publish(int resourceId) {
        boolean sent = streamBridge.send(BINDING_NAME, new ResourceUploadedEvent(resourceId));
        if (sent) {
            log.info("Published resource-uploaded event for resourceId={}", resourceId);
        } else {
            log.error("Failed to publish resource-uploaded event for resourceId={}", resourceId);
        }
    }
}
