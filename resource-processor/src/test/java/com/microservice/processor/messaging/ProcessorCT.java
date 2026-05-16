package com.microservice.processor.messaging;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.microservice.processor.dto.ResourceUploadedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Component test for the resource-processor messaging pipeline. The full Spring context runs
 * against a real RabbitMQ container; outbound HTTP to resource-service and song-service is
 * intercepted by WireMock. Eureka discovery is replaced by a mock that returns WireMock's base URL
 * for every service lookup.
 *
 * HOW TO RUN:
 *   mvn verify -pl resource-processor -DskipCTs=false
 *   Requires Docker to be running.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.stream.bindings.processResourceUploaded-in-0.consumer.max-attempts=1"
})
class ProcessorCT {

    private static final String EXCHANGE = "resource-uploaded";

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @MockitoBean
    DiscoveryClient discoveryClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @BeforeEach
    void stubDiscovery() {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getUri()).thenReturn(URI.create(wireMock.baseUrl()));
        when(discoveryClient.getInstances("resource-service")).thenReturn(List.of(instance));
        when(discoveryClient.getInstances("song-service")).thenReturn(List.of(instance));
    }

    @Test
    void processor_receivesEvent_fetchesAudioFromResourceService() {
        wireMock.stubFor(get(urlEqualTo("/resources/5"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "audio/mpeg")
                        .withBody(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x00})));
        wireMock.stubFor(post(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":5}")));

        publishEvent(5);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                wireMock.verify(getRequestedFor(urlEqualTo("/resources/5"))));
    }

    @Test
    void processor_receivesEvent_sendsExtractedMetadataToSongService() {
        wireMock.stubFor(get(urlEqualTo("/resources/7"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "audio/mpeg")
                        .withBody(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x00})));
        wireMock.stubFor(post(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":7}")));

        publishEvent(7);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                wireMock.verify(postRequestedFor(urlPathEqualTo("/songs"))));
    }

    @Test
    void processor_receivesEvent_payloadContainsResourceId() {
        wireMock.stubFor(get(urlEqualTo("/resources/42"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "audio/mpeg")
                        .withBody(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x00})));
        wireMock.stubFor(post(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":42}")));

        publishEvent(42);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                wireMock.verify(postRequestedFor(urlPathEqualTo("/songs"))
                        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("\"id\":42"))));
    }

    @Test
    void processor_emptyAudioBytes_stillSendsMetadataWithDefaults() {
        wireMock.stubFor(get(urlEqualTo("/resources/99"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "audio/mpeg")
                        .withBody(new byte[0])));
        wireMock.stubFor(post(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":99}")));

        publishEvent(99);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                wireMock.verify(postRequestedFor(urlPathEqualTo("/songs"))
                        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("Unknown"))
                        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("0000"))));
    }

    private void publishEvent(int resourceId) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        org.springframework.amqp.core.Message message =
                converter.toMessage(new ResourceUploadedEvent(resourceId), props);
        rabbitTemplate.send(EXCHANGE, "", message);
    }
}