package com.microservice.resource.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.microservice.resource.dto.DeleteResourcesResponseDto;
import com.microservice.resource.dto.ResourceIdResponseDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Component test for ResourceController — runs the full resource-service Spring context against
 * real PostgreSQL, LocalStack S3, and RabbitMQ containers. The song-service peer is stubbed
 * via WireMock; Eureka discovery is replaced by a mock that returns WireMock's URL.
 * <p>
 * HOW TO RUN:
 * mvn verify -pl resource-service -DskipCTs=false
 * Requires Docker to be running.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "aws.s3.bucket-name=test-bucket",
        "aws.s3.access-key=test",
        "aws.s3.secret-key=test"
})
class ResourceControllerCT {

    private static final String BUCKET = "test-bucket";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8.1"))
            .withServices(LocalStackContainer.Service.S3);

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("aws.s3.endpoint",
                () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    }

    @BeforeAll
    static void createBucket() {
        S3Client s3 = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
        s3.createBucket(b -> b.bucket(BUCKET));
    }

    @MockitoBean
    DiscoveryClient discoveryClient;

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void stubSongServiceDiscovery() {
        ServiceInstance songInstance = mock(ServiceInstance.class);
        when(songInstance.getUri()).thenReturn(URI.create(wireMock.baseUrl()));
        when(discoveryClient.getInstances("song-service")).thenReturn(List.of(songInstance));

        wireMock.stubFor(delete(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withBody("{\"ids\":[]}")));
    }

    @Test
    void postResource_validMp3_returns200WithId() {
        ResponseEntity<ResourceIdResponseDto> response = uploadMinimalMp3("test.mp3");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
    }

    @Test
    void postResource_validMp3_fileIsRetrievableFromS3() {
        ResponseEntity<ResourceIdResponseDto> upload = uploadMinimalMp3("retrievable.mp3");
        int id = upload.getBody().getId();

        ResponseEntity<byte[]> get = restTemplate.getForEntity("/resources/" + id, byte[].class);

        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
        assertThat(get.getBody()).isNotEmpty();
    }

    @Test
    void postResource_wrongContentType_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/resources", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getResource_existingId_returns200WithAudioMpegContentType() {
        int id = uploadMinimalMp3("audio.mp3").getBody().getId();

        ResponseEntity<byte[]> response = restTemplate.getForEntity("/resources/" + id, byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
    }

    @Test
    void getResource_nonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/resources/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getResource_nonNumericId_returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity("/resources/abc", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteResource_existingId_returns200WithIdAndCallsSongService() {
        int id = uploadMinimalMp3("to-delete.mp3").getBody().getId();
        wireMock.stubFor(delete(urlPathEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"ids\":[" + id + "]}")));

        ResponseEntity<DeleteResourcesResponseDto> response = restTemplate.exchange(
                "/resources?id=" + id, HttpMethod.DELETE, null, DeleteResourcesResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIds()).contains(id);
        wireMock.verify(deleteRequestedFor(urlPathEqualTo("/songs"))
                .withQueryParam("id", WireMock.equalTo(String.valueOf(id))));
    }

    @Test
    void deleteResource_existingId_fileGoneFromS3() {
        int id = uploadMinimalMp3("gone.mp3").getBody().getId();

        restTemplate.exchange("/resources?id=" + id, HttpMethod.DELETE, null, DeleteResourcesResponseDto.class);

        ResponseEntity<String> get = restTemplate.getForEntity("/resources/" + id, String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteResource_nonExistentId_returns200WithEmptyListAndDoesNotCallSongService() {
        ResponseEntity<DeleteResourcesResponseDto> response = restTemplate.exchange(
                "/resources?id=99999", HttpMethod.DELETE, null, DeleteResourcesResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIds()).isEmpty();
        wireMock.verify(0, deleteRequestedFor(urlPathEqualTo("/songs")));
    }

    private ResponseEntity<ResourceIdResponseDto> uploadMinimalMp3(String filename) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audioFile", new ByteArrayResource(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x00}) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity("/resources", new HttpEntity<>(body, headers),
                ResourceIdResponseDto.class);
    }
}