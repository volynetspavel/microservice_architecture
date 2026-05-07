package com.microservice.resource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration test for CloudStorageService against a real LocalStack S3.
 * Does not load a Spring Boot context — beans are constructed manually.
 *
 * HOW TO RUN:
 *   mvn test -pl resource-service -Dtest=CloudStorageServiceIT
 *   Requires Docker to be running on the host machine.
 */
@Testcontainers
class CloudStorageServiceIT {

    private static final String BUCKET = "test-bucket";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8.1"))
            .withServices(S3);

    private CloudStorageService cloudStorageService;

    @BeforeEach
    void setUp() {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(localStack.getRegion()))
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        s3Client.createBucket(b -> b.bucket(BUCKET));

        cloudStorageService = new CloudStorageService(s3Client);
        ReflectionTestUtils.setField(cloudStorageService, "bucketName", BUCKET);
    }

    @Test
    void uploadAudioFile_storesFileAndReturnsCorrectLocation() {
        byte[] content = "mp3-data".getBytes();

        String location = cloudStorageService.uploadAudioFile("1_track.mp3", content);

        assertThat(location).isEqualTo("songs/1_track.mp3");
    }

    @Test
    void downloadFile_afterUpload_returnsOriginalContent() {
        byte[] content = "audio-bytes".getBytes();
        String location = cloudStorageService.uploadAudioFile("2_track.mp3", content);

        byte[] downloaded = cloudStorageService.downloadFile(location);

        assertThat(downloaded).isEqualTo(content);
    }

    @Test
    void deleteFile_afterUpload_fileIsNoLongerRetrievable() {
        byte[] content = "data".getBytes();
        String location = cloudStorageService.uploadAudioFile("3_track.mp3", content);

        cloudStorageService.deleteFile(location);

        assertThatThrownBy(() -> cloudStorageService.downloadFile(location))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void uploadMultipleFiles_eachStoredUnderCorrectKey() {
        cloudStorageService.uploadAudioFile("4_a.mp3", "data-a".getBytes());
        cloudStorageService.uploadAudioFile("4_b.mp3", "data-b".getBytes());

        byte[] a = cloudStorageService.downloadFile("songs/4_a.mp3");
        byte[] b = cloudStorageService.downloadFile("songs/4_b.mp3");

        assertThat(new String(a)).isEqualTo("data-a");
        assertThat(new String(b)).isEqualTo("data-b");
    }

    @Test
    void downloadFile_nonExistentKey_throwsException() {
        assertThatThrownBy(() -> cloudStorageService.downloadFile("songs/missing.mp3"))
                .isInstanceOf(RuntimeException.class);
    }
}
