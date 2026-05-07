package com.microservice.processor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EurekaServiceResolverTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @InjectMocks
    private EurekaServiceResolver resolver;

    @Test
    void resolve_serviceHasInstances_returnsFirstInstanceUri() {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getUri()).thenReturn(URI.create("http://song-service:8082"));
        when(discoveryClient.getInstances("song-service")).thenReturn(List.of(instance));

        String result = resolver.resolve("song-service");

        assertThat(result).isEqualTo("http://song-service:8082");
    }

    @Test
    void resolve_multipleInstances_returnsFirstOne() {
        ServiceInstance first = mock(ServiceInstance.class);
        ServiceInstance second = mock(ServiceInstance.class);
        when(first.getUri()).thenReturn(URI.create("http://song-service:8082"));
        when(discoveryClient.getInstances("song-service")).thenReturn(List.of(first, second));

        String result = resolver.resolve("song-service");

        assertThat(result).isEqualTo("http://song-service:8082");
    }

    @Test
    void resolve_noInstancesRegistered_throwsIllegalStateException() {
        when(discoveryClient.getInstances("resource-service")).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("resource-service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("resource-service");
    }

    @Test
    void resolve_serviceNameIsIncludedInErrorMessage() {
        when(discoveryClient.getInstances("unknown-service")).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("unknown-service"))
                .hasMessageContaining("unknown-service");
    }
}
