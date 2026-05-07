package com.microservice.processor.messaging;

import com.microservice.processor.dto.ResourceUploadedEvent;
import com.microservice.processor.dto.SongCreateRequestDto;
import com.microservice.processor.service.ResourceServiceClient;
import com.microservice.processor.service.SongServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test verifying that ResourceUploadedConsumer receives a message from RabbitMQ
 * and drives the full processing pipeline (fetch audio → extract metadata → send to song service).
 * HTTP dependencies (ResourceServiceClient, SongServiceClient) are mocked so this test
 * focuses solely on the messaging binding and consumer wiring.
 *
 * HOW TO RUN:
 *   mvn test -pl resource-processor -Dtest=ResourceUploadedConsumerIT
 *   Requires Docker to be running on the host machine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Testcontainers
class ResourceUploadedConsumerIT {

    private static final String EXCHANGE = "resource-uploaded";
    private static final int TIMEOUT_MS = 10_000;

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");

    @MockitoBean
    private ResourceServiceClient resourceServiceClient;

    @MockitoBean
    private SongServiceClient songServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void consumer_receivesEvent_fetchesAudioFromResourceService() {
        byte[] fakeAudio = new byte[]{1, 2, 3};
        when(resourceServiceClient.getResource(5)).thenReturn(fakeAudio);

        publishEvent(5);

        verify(resourceServiceClient, timeout(TIMEOUT_MS)).getResource(5);
    }

    @Test
    void consumer_receivesEvent_sendsExtractedMetadataToSongService() {
        byte[] fakeAudio = new byte[]{};
        when(resourceServiceClient.getResource(10)).thenReturn(fakeAudio);

        publishEvent(10);

        // Mp3MetadataExtractor returns defaults for empty audio data;
        // the test verifies the pipeline completes and sendMetadata is called
        verify(songServiceClient, timeout(TIMEOUT_MS)).sendMetadata(any(SongCreateRequestDto.class));
    }

    @Test
    void consumer_receivesEvent_usesCorrectResourceIdForFetch() {
        int resourceId = 99;
        when(resourceServiceClient.getResource(resourceId)).thenReturn(new byte[]{});

        publishEvent(resourceId);

        verify(resourceServiceClient, timeout(TIMEOUT_MS)).getResource(eq(resourceId));
    }

    private void publishEvent(int resourceId) {
        org.springframework.amqp.core.Message message;
        try {
            Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            message = converter.toMessage(new ResourceUploadedEvent(resourceId), props);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        rabbitTemplate.send(EXCHANGE, "", message);
    }
}
