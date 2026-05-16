package com.microservice.song.service;

import com.microservice.song.dto.DeleteSongsResponseDto;
import com.microservice.song.dto.SongCreateRequestDto;
import com.microservice.song.dto.SongIdResponseDto;
import com.microservice.song.dto.SongResponseDto;
import com.microservice.song.entity.Song;
import com.microservice.song.exception.InvalidRequestException;
import com.microservice.song.exception.SongAlreadyExistsException;
import com.microservice.song.exception.SongNotFoundException;
import com.microservice.song.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository repository;

    @InjectMocks
    private SongService songService;

    // --- createSong ---

    @Test
    void createSong_newId_savesSongAndReturnsId() {
        SongCreateRequestDto request = new SongCreateRequestDto(1, "Song A", "Artist A", "Album A", "03:45", "2020");
        Song savedSong = new Song(1, "Song A", "Artist A", "Album A", "03:45", "2020");

        when(repository.existsById(1)).thenReturn(false);
        when(repository.save(any(Song.class))).thenReturn(savedSong);

        SongIdResponseDto result = songService.createSong(request);

        assertThat(result.getId()).isEqualTo(1);
        verify(repository).save(any(Song.class));
    }

    @Test
    void createSong_duplicateId_throwsSongAlreadyExistsException() {
        SongCreateRequestDto request = new SongCreateRequestDto(1, "Song A", "Artist A", "Album A", "03:45", "2020");
        when(repository.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> songService.createSong(request))
                .isInstanceOf(SongAlreadyExistsException.class)
                .hasMessageContaining("1");

        verify(repository, never()).save(any());
    }

    // --- getSongById ---

    @Test
    void getSongById_validId_returnsSongResponse() {
        Song song = new Song(5, "Title", "Artist", "Album", "04:00", "2019");
        when(repository.findById(5)).thenReturn(Optional.of(song));

        SongResponseDto result = songService.getSongById("5");

        assertThat(result.getId()).isEqualTo(5);
        assertThat(result.getName()).isEqualTo("Title");
        assertThat(result.getArtist()).isEqualTo("Artist");
        assertThat(result.getAlbum()).isEqualTo("Album");
        assertThat(result.getDuration()).isEqualTo("04:00");
        assertThat(result.getYear()).isEqualTo("2019");
    }

    @Test
    void getSongById_unknownId_throwsSongNotFoundException() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> songService.getSongById("99"))
                .isInstanceOf(SongNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getSongById_nonNumericId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.getSongById("abc"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getSongById_zeroId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.getSongById("0"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getSongById_negativeId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.getSongById("-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- deleteSongs ---

    @Test
    void deleteSongs_existingIds_deletesAndReturnsIds() {
        when(repository.existsById(1)).thenReturn(true);
        when(repository.existsById(2)).thenReturn(true);

        DeleteSongsResponseDto result = songService.deleteSongs("1,2");

        assertThat(result.getIds()).containsExactlyInAnyOrder(1, 2);
        verify(repository).deleteById(1);
        verify(repository).deleteById(2);
    }

    @Test
    void deleteSongs_mixedIds_skipsNonExistentAndReturnsOnlyDeleted() {
        when(repository.existsById(1)).thenReturn(true);
        when(repository.existsById(99)).thenReturn(false);

        DeleteSongsResponseDto result = songService.deleteSongs("1,99");

        assertThat(result.getIds()).containsExactly(1);
        verify(repository).deleteById(1);
        verify(repository, never()).deleteById(99);
    }

    @Test
    void deleteSongs_emptyCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.deleteSongs(""))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteSongs_nullCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.deleteSongs(null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteSongs_csvExceedsMaxLength_throwsInvalidRequestException() {
        String longCsv = "1,".repeat(100) + "1"; // well over 200 chars
        assertThatThrownBy(() -> songService.deleteSongs(longCsv))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("200");
    }

    @Test
    void deleteSongs_nonNumericId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> songService.deleteSongs("1,abc"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteSongs_withSpacesAroundIds_parsesCorrectly() {
        when(repository.existsById(1)).thenReturn(true);
        when(repository.existsById(2)).thenReturn(true);

        DeleteSongsResponseDto result = songService.deleteSongs("1, 2");

        assertThat(result.getIds()).containsExactlyInAnyOrder(1, 2);
    }
}
