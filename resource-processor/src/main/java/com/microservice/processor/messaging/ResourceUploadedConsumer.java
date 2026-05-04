package com.microservice.processor.messaging;

import com.microservice.processor.dto.ResourceUploadedEvent;
import com.microservice.processor.service.Mp3MetadataExtractor;
import com.microservice.processor.service.ResourceServiceClient;
import com.microservice.processor.service.SongServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Listens for ResourceUploadedEvent messages from RabbitMQ, retrieves the associated audio data,
 * extracts metadata using Mp3MetadataExtractor, and sends the metadata to the Song Service.
 * Implements the Consumer interface to be used as a Spring Cloud Stream function bean.
 */
@Slf4j
@Component
public class ResourceUploadedConsumer implements Consumer<ResourceUploadedEvent> {

    private final Mp3MetadataExtractor mp3MetadataExtractor;
    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;

    public ResourceUploadedConsumer(Mp3MetadataExtractor mp3MetadataExtractor,
                                    ResourceServiceClient resourceServiceClient,
                                    SongServiceClient songServiceClient) {
        this.mp3MetadataExtractor = mp3MetadataExtractor;
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
    }

    @Override
    public void accept(ResourceUploadedEvent event) {
        int resourceId = event.getResourceId();
        log.info("Received resource-uploaded event for resource id {}", resourceId);
        try {
            byte[] audioData = resourceServiceClient.getResource(resourceId);
            songServiceClient.sendMetadata(mp3MetadataExtractor.extractMetadata(resourceId, audioData));
            log.info("Song metadata saved for resource id {}", resourceId);
        } catch (Exception e) {
            log.error("Failed to process resource id {}: {}", resourceId, e.getMessage());
        }
    }
}