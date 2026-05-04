package com.microservice.processor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Client for communicating with Resource Service to fetch audio data.
 * Uses Spring's DiscoveryClient to find Resource Service instances registered in Eureka.
 * Uses RestClient to make HTTP requests to Resource Service.
 */
@Slf4j
@Service
public class ResourceServiceClient {

    private static final String SERVICE_NAME = "resource-service";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public ResourceServiceClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
    }

    /**
     * Fetches audio data for a given resource ID from Resource Service.
     * @param id the ID of the resource to fetch
     * @return the audio data as a byte array
     */
    public byte[] getResource(int id) {
        String baseUrl = resolveBaseUrl();
        log.info("Fetching resource {} from {}", id, baseUrl);
        return restClient.get()
                .uri(baseUrl + "/resources/{id}", id)
                .accept(MediaType.parseMediaType("audio/mpeg"))
                .retrieve()
                .body(byte[].class);
    }

    /**
     * Resolves the base URL of the Resource Service by querying Eureka for available instances.
     * @return the base URL of the Resource Service instance to use for requests
     */
    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_NAME);
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances of " + SERVICE_NAME + " found in Eureka");
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }
}