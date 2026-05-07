package com.microservice.processor.service;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

/**
 * Utility class to resolve service URLs from Eureka by service name.
 * This allows the processor service to dynamically discover the resource
 * and song services without hardcoding their URLs
 */
@Component
public class EurekaServiceResolver {

    private final DiscoveryClient discoveryClient;

    public EurekaServiceResolver(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public String resolve(String serviceName) {
        return discoveryClient.getInstances(serviceName).stream()
                .findFirst()
                .map(instance -> instance.getUri().toString())
                .orElseThrow(() -> new IllegalStateException("No instances of " + serviceName + " found in Eureka"));
    }
}
