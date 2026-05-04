package com.microservice.processor.messaging;

import com.microservice.processor.dto.AudioMetadataDto;
import com.microservice.processor.dto.ResourceUploadedEvent;
import com.microservice.processor.dto.SongCreateRequestDto;
import com.microservice.processor.service.ProcessorService;
import com.microservice.processor.service.ResourceServiceClient;
import com.microservice.processor.service.SongServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Consumer for processing "resource-uploaded" events from RabbitMQ.
 * Listens for events containing the resource ID of the uploaded audio file,
 * retrieves the audio data from the Resource Service, extracts metadata
 * using the Processor Service, and then creates a new song entry in the
 * Song Service with the extracted metadata. Handles errors gracefully and
 * logs all processing steps for monitoring and debugging purposes.
 */
@Slf4j
@Configuration
public class ResourceUploadedConsumer {

    private final ProcessorService processorService;
    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;

    public ResourceUploadedConsumer(ProcessorService processorService,
                                    ResourceServiceClient resourceServiceClient,
                                    SongServiceClient songServiceClient) {
        this.processorService = processorService;
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
    }

    /**
     * Defines a Consumer bean that listens for "resource-uploaded" events.
     * @return A Consumer that processes ResourceUploadedEvent messages.
     */
    @Bean
    public Consumer<ResourceUploadedEvent> processResourceUploaded() {
        return event -> {
            int resourceId = event.getResourceId();
            log.info("Received resource-uploaded event for resource id {}", resourceId);
            try {
                byte[] audioData = resourceServiceClient.getResource(resourceId);
                AudioMetadataDto metadataDto = processorService.processResource(resourceId, audioData);
                SongCreateRequestDto songDto = toSongCreateRequest(resourceId, metadataDto.getMetadata());
                songServiceClient.createSong(songDto);
                log.info("Song metadata saved for resource id {}", resourceId);
            } catch (Exception e) {
                log.error("Failed to process resource id {}: {}", resourceId, e.getMessage());
            }
        };
    }

    private SongCreateRequestDto toSongCreateRequest(int id, Map<String, String> metadata) {
        String name = metadata.getOrDefault("name", "Unknown");
        String artist = metadata.getOrDefault("artist", "Unknown");
        String album = metadata.getOrDefault("album", "Unknown");
        String duration = sanitizeDuration(metadata.get("duration"));
        String year = sanitizeYear(metadata.get("year"));
        return new SongCreateRequestDto(id, name, artist, album, duration, year);
    }

    // Song Service requires MM:SS format (^\d{2}:[0-5]\d$)
    private String sanitizeDuration(String duration) {
        if (duration != null && duration.matches("^\\d{2}:[0-5]\\d$")) {
            return duration;
        }
        return "00:00";
    }

    // Song Service requires YYYY format (^\d{4}$)
    private String sanitizeYear(String year) {
        if (year != null && year.matches("^\\d{4}$")) {
            return year;
        }
        return "0000";
    }
}