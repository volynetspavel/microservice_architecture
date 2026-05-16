package com.microservice.song.repository;

import com.microservice.song.entity.Song;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SongRepository against a real PostgreSQL database.
 *
 * HOW TO RUN:
 *   mvn test -pl song-service -Dtest=SongRepositoryIT
 *   Requires Docker to be running on the host machine.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class SongRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SongRepository repository;

    @Test
    void save_newSong_canBeRetrievedById() {
        Song song = new Song(1, "Title", "Artist", "Album", "03:45", "2020");
        repository.save(song);

        Optional<Song> found = repository.findById(1);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Title");
        assertThat(found.get().getArtist()).isEqualTo("Artist");
        assertThat(found.get().getAlbum()).isEqualTo("Album");
        assertThat(found.get().getDuration()).isEqualTo("03:45");
        assertThat(found.get().getYear()).isEqualTo("2020");
    }

    @Test
    void existsById_savedSong_returnsTrue() {
        repository.save(new Song(2, "Another", "Artist", "Album", "02:30", "2021"));

        assertThat(repository.existsById(2)).isTrue();
    }

    @Test
    void existsById_nonExistentId_returnsFalse() {
        assertThat(repository.existsById(999)).isFalse();
    }

    @Test
    void deleteById_savedSong_removesItFromDatabase() {
        repository.save(new Song(3, "ToDelete", "Artist", "Album", "01:00", "2022"));

        repository.deleteById(3);

        assertThat(repository.existsById(3)).isFalse();
    }

    @Test
    void findById_afterDeletion_returnsEmpty() {
        repository.save(new Song(4, "Song", "Artist", "Album", "02:00", "2019"));
        repository.deleteById(4);

        Optional<Song> result = repository.findById(4);

        assertThat(result).isEmpty();
    }

    @Test
    void save_multipleSongs_allPersisted() {
        repository.save(new Song(10, "Song A", "Artist", "Album", "03:00", "2020"));
        repository.save(new Song(11, "Song B", "Artist", "Album", "04:00", "2021"));
        repository.save(new Song(12, "Song C", "Artist", "Album", "05:00", "2022"));

        assertThat(repository.count()).isEqualTo(3);
    }
}
