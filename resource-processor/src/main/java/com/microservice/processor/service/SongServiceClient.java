package com.microservice.processor.service;

import com.microservice.processor.dto.SongCreateRequestDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for communicating with the Song Service to save MP3 metadata.
 */
@Service
public class SongServiceClient {

    private static final String SERVICE_NAME = "song-service";
    private static final String SONGS_URL = "/songs";

    private final EurekaServiceResolver eurekaServiceResolver;
    private final RestClient restClient;

    public SongServiceClient(EurekaServiceResolver eurekaServiceResolver, RestClient.Builder builder) {
        this.eurekaServiceResolver = eurekaServiceResolver;
        this.restClient = builder.build();
    }

    public void sendMetadata(SongCreateRequestDto dto) {
        String baseUrl = eurekaServiceResolver.resolve(SERVICE_NAME);
        restClient.post()
                .uri(baseUrl + SONGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }
}
