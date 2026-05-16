package com.microservice.song.controller;

import com.microservice.song.dto.DeleteSongsResponseDto;
import com.microservice.song.dto.SongCreateRequestDto;
import com.microservice.song.dto.SongIdResponseDto;
import com.microservice.song.dto.SongResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Component test for SongController — starts the full Spring context against a real PostgreSQL
 * container and exercises every endpoint through HTTP. No mocks, no peer services needed
 * (song-service is self-contained).
 *
 * HOW TO RUN:
 *   mvn verify -pl song-service -DskipCTs=false
 *   Requires Docker to be running.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class SongControllerCT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void postSong_validBody_returns200WithId() {
        SongCreateRequestDto request = new SongCreateRequestDto(1, "Test Song", "Artist", "Album", "03:45", "2020");

        ResponseEntity<SongIdResponseDto> response = restTemplate.postForEntity("/songs", request, SongIdResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
    }

    @Test
    void postSong_missingRequiredField_returns400WithValidationDetails() {
        SongCreateRequestDto request = new SongCreateRequestDto(2, "", "Artist", "Album", "03:45", "2020");

        ResponseEntity<String> response = restTemplate.postForEntity("/songs", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("name");
    }

    @Test
    void postSong_duplicateId_returns409() {
        SongCreateRequestDto request = new SongCreateRequestDto(5, "Song", "Artist", "Album", "01:00", "2021");
        restTemplate.postForEntity("/songs", request, SongIdResponseDto.class);

        ResponseEntity<String> second = restTemplate.postForEntity("/songs", request, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getSong_existingId_returns200WithAllFields() {
        SongCreateRequestDto request = new SongCreateRequestDto(10, "My Song", "My Artist", "My Album", "02:30", "2019");
        restTemplate.postForEntity("/songs", request, SongIdResponseDto.class);

        ResponseEntity<SongResponseDto> response = restTemplate.getForEntity("/songs/10", SongResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SongResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(10);
        assertThat(body.getName()).isEqualTo("My Song");
        assertThat(body.getArtist()).isEqualTo("My Artist");
        assertThat(body.getAlbum()).isEqualTo("My Album");
        assertThat(body.getDuration()).isEqualTo("02:30");
        assertThat(body.getYear()).isEqualTo("2019");
    }

    @Test
    void getSong_nonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/songs/999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getSong_nonNumericId_returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity("/songs/abc", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteSongs_bothExist_returns200WithBothIds() {
        restTemplate.postForEntity("/songs",
                new SongCreateRequestDto(20, "Song A", "Artist", "Album", "01:00", "2020"),
                SongIdResponseDto.class);
        restTemplate.postForEntity("/songs",
                new SongCreateRequestDto(21, "Song B", "Artist", "Album", "01:00", "2020"),
                SongIdResponseDto.class);

        ResponseEntity<DeleteSongsResponseDto> response = restTemplate.exchange(
                "/songs?id=20,21", HttpMethod.DELETE, null, DeleteSongsResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIds()).containsExactlyInAnyOrder(20, 21);
    }

    @Test
    void deleteSongs_nonExistentId_returns200WithEmptyList() {
        ResponseEntity<DeleteSongsResponseDto> response = restTemplate.exchange(
                "/songs?id=99", HttpMethod.DELETE, null, DeleteSongsResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIds()).isEmpty();
    }
}