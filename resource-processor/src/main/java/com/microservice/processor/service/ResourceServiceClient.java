package com.microservice.processor.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for communicating with the Resource Service to retrieve audio data by resource ID.
 */
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

    public byte[] getResource(int id) {
        String baseUrl = eurekaServiceResolver.resolve(SERVICE_NAME);
        return restClient.get()
                .uri(baseUrl + GET_RESOURCE_URL, id)
                .accept(MediaType.parseMediaType("audio/mpeg"))
                .retrieve()
                .body(byte[].class);
    }
}
