package com.microservice.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Client for communicating with Song Service to save MP3 metadata.
 */
@Slf4j
@Service
public class SongServiceClient {

    private static final String SONG_SERVICE = "song-service";
    private static final String SONGS_URL = "/songs";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public SongServiceClient(DiscoveryClient discoveryClient, RestClient.Builder restClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Deletes metadata from Song Service by resource ID.
     *
     * @param id The ID of the metadata to delete.
     */
    public void deleteMetadata(Integer id) {
        ServiceInstance instance = getServiceInstance();
        try {
            String metadataId = restClient.delete()
                    .uri(instance.getUri() + SONGS_URL + "?id=" + id)
                    .retrieve()
                    .body(String.class);
            log.info("Metadata with ID={} successfully deleted from Song Service", metadataId);
        } catch (RestClientException e) {
            log.error("Failed to delete metadata from Song Service: {}", e.getMessage());
        }
    }

    /**
     * Retrieves a ServiceInstance for the Song Service from Eureka.
     */
    private ServiceInstance getServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SONG_SERVICE);
        return instances.stream()
                .findFirst()
                .orElseThrow(() -> new RestClientException("Song Service instance not found"));
    }
}

