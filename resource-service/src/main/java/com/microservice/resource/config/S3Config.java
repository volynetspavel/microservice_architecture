package com.microservice.resource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuration class for AWS S3 client.
 */
@Configuration
public class S3Config {
    @Value("${aws.s3.endpoint}")
    private String endpoint;
    @Value("{aws.s3.access-key}")
    private String accessKey;
    @Value("{aws.s3.secret-key}")
    private String secretKey;
    @Value("${aws.s3.region}")
    private String region;

    /**
     * Configures and creates an S3Client bean for interacting with AWS S3.
     * @return the configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();
    }
}
