package com.microservice.processor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for communicating with the Resource Service to retrieve audio data by resource ID.
 */
@Slf4j
@Service
public class ResourceServiceClient {

    private static final String SERVICE_NAME = "resource-service";
    private static final String GET_RESOURCE_URL = "/resources/{id}";

    private final EurekaServiceResolver eurekaServiceResolver;
    private final RestClient restClient;

    public ResourceServiceClient(EurekaServiceResolver eurekaServiceResolver, RestClient.Builder builder) {
        this.eurekaServiceResolver = eurekaServiceResolver;
        this.restClient = builder.build();
    }

    @Retryable(
            retryFor = {RestClientException.class, IllegalStateException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000),
            listeners = "retryLoggingListener"
    )
    public byte[] getResource(int id) {
        String baseUrl = eurekaServiceResolver.resolve(SERVICE_NAME);
        log.info("Fetching resource {} from {}", id, baseUrl);
        return restClient.get()
                .uri(baseUrl + GET_RESOURCE_URL, id)
                .accept(MediaType.parseMediaType("audio/mpeg"))
                .retrieve()
                .body(byte[].class);
    }
}