package com.microservice.processor.service;

import com.microservice.processor.dto.SongCreateRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class SongServiceClient {

    private static final String SERVICE_NAME = "song-service";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public SongServiceClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
    }

    public void createSong(SongCreateRequestDto dto) {
        String baseUrl = resolveBaseUrl();
        log.info("Saving song metadata for resource id {} to {}", dto.getId(), baseUrl);
        restClient.post()
                .uri(baseUrl + "/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_NAME);
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances of " + SERVICE_NAME + " found in Eureka");
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }
}