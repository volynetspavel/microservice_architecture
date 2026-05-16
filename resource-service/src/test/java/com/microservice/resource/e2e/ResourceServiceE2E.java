package com.microservice.resource.e2e;

import com.microservice.resource.dto.DeleteResourcesResponseDto;
import com.microservice.resource.dto.ResourceIdResponseDto;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E smoke tests for the three core business flows.
 * Requires the full Docker Compose stack to be running before execution.
 * Use run-e2e-test.sh to start the stack and run these tests.
 */
class ResourceServiceE2E {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceE2E.class);

    private static final String RESOURCE_SERVICE = "http://localhost:8081";
    private static final String SONG_SERVICE = "http://localhost:8082";

    private final RestTemplate rest = buildRestTemplate();

    @Test
    void upload_storesResourceAndMetadata() {
        log.info("[upload] Uploading MP3 to resource-service...");
        int id = uploadMp3();
        log.info("[upload] Resource created with id={}", id);

        log.info("[upload] Polling song-service until metadata appears for id={}...", id);
        awaitSongAppears(id);

        ResponseEntity<Map> song = rest.getForEntity(SONG_SERVICE + "/songs/" + id, Map.class);
        assertThat(song.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(song.getBody()).isNotNull();
        assertThat((String) song.getBody().get("name")).isNotBlank();
        log.info("[upload] OK — song metadata: name='{}', artist='{}', album='{}', duration='{}', year='{}'",
                song.getBody().get("name"), song.getBody().get("artist"),
                song.getBody().get("album"), song.getBody().get("duration"), song.getBody().get("year"));
    }

    @Test
    void retrieval_returnsBinaryMp3() {
        log.info("[retrieval] Uploading MP3 to resource-service...");
        int id = uploadMp3();
        log.info("[retrieval] Resource created with id={}, fetching binary...", id);

        ResponseEntity<byte[]> response = rest.getForEntity(RESOURCE_SERVICE + "/resources/" + id, byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
        assertThat(response.getBody()).isNotEmpty();
        log.info("[retrieval] OK — status={}, Content-Type={}, body={} bytes",
                response.getStatusCode(), response.getHeaders().getContentType(),
                response.getBody().length);
    }

    @Test
    void delete_removesResourceAndMetadata() {
        log.info("[delete] Uploading MP3 to resource-service...");
        int id = uploadMp3();
        log.info("[delete] Resource created with id={}", id);

        log.info("[delete] Polling song-service until metadata appears for id={}...", id);
        awaitSongAppears(id);

        log.info("[delete] Deleting resource id={}...", id);
        ResponseEntity<DeleteResourcesResponseDto> deleteResp = rest.exchange(
                RESOURCE_SERVICE + "/resources?id=" + id,
                HttpMethod.DELETE, null, DeleteResourcesResponseDto.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResp.getBody()).isNotNull();
        assertThat(deleteResp.getBody().getIds()).contains(id);
        log.info("[delete] DELETE returned ids={}", deleteResp.getBody().getIds());

        ResponseEntity<byte[]> resourceResp = rest.getForEntity(RESOURCE_SERVICE + "/resources/" + id, byte[].class);
        assertThat(resourceResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        log.info("[delete] OK — GET /resources/{} → {}", id, resourceResp.getStatusCode());

        ResponseEntity<Map> songResp = rest.getForEntity(SONG_SERVICE + "/songs/" + id, Map.class);
        assertThat(songResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        log.info("[delete] OK — GET /songs/{} → {}", id, songResp.getStatusCode());
    }

    private int uploadMp3() {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audioFile", new ClassPathResource("mp3/valid-sample-with-required-tags.mp3"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<ResourceIdResponseDto> response = rest.postForEntity(
                RESOURCE_SERVICE + "/resources",
                new HttpEntity<>(body, headers),
                ResourceIdResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        return response.getBody().getId();
    }

    private void awaitSongAppears(int id) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .conditionEvaluationListener(condition ->
                        log.info("[await] song id={} — elapsed {}s, matched={}",
                                id, condition.getElapsedTimeInMS() / 1000, condition.isSatisfied()))
                .until(() -> rest.getForEntity(SONG_SERVICE + "/songs/" + id, Map.class)
                        .getStatusCode() == HttpStatus.OK);
    }

    private static RestTemplate buildRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
        return template;
    }
}