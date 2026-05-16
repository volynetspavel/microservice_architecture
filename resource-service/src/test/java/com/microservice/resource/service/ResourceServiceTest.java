package com.microservice.resource.service;

import com.microservice.resource.dto.DeleteResourcesResponseDto;
import com.microservice.resource.dto.ResourceDataResponseDto;
import com.microservice.resource.dto.ResourceIdResponseDto;
import com.microservice.resource.entity.Resource;
import com.microservice.resource.exception.InvalidRequestException;
import com.microservice.resource.exception.ResourceNotFoundException;
import com.microservice.resource.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private CloudStorageService cloudStorageService;

    @Mock
    private ResourceUploadedEventPublisher eventPublisher;

    @InjectMocks
    private ResourceService resourceService;

    // --- uploadResource ---

    @Test
    void uploadResource_validFile_savesToStorageAndReturnsId() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("track.mp3");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});

        Resource savedPlaceholder = new Resource(7, "");
        when(repository.save(any(Resource.class))).thenReturn(savedPlaceholder);
        when(cloudStorageService.uploadAudioFile(anyString(), any())).thenReturn("songs/7_track.mp3");

        ResourceIdResponseDto result = resourceService.uploadResource(file);

        assertThat(result.getId()).isEqualTo(7);
        verify(cloudStorageService).uploadAudioFile(eq("7_track.mp3"), any());
        verify(eventPublisher).publish(7);
        verify(repository, times(2)).save(any(Resource.class));
    }

    @Test
    void uploadResource_nullFile_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.uploadResource(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("null");
    }

    @Test
    void uploadResource_fileReadThrowsIoException_throwsInvalidRequestException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("track.mp3");
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> resourceService.uploadResource(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("invalid");
    }

    // --- getResourceById ---

    @Test
    void getResourceById_validId_returnsAudioData() {
        Resource resource = new Resource(3, "songs/3_track.mp3");
        when(repository.findById(3)).thenReturn(Optional.of(resource));
        when(cloudStorageService.downloadFile("songs/3_track.mp3")).thenReturn(new byte[]{10, 20});

        ResourceDataResponseDto result = resourceService.getResourceById("3");

        assertThat(result.getAudioData()).containsExactly(10, 20);
    }

    @Test
    void getResourceById_unknownId_throwsResourceNotFoundException() {
        when(repository.findById(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceById("42"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void getResourceById_nonNumericId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.getResourceById("xyz"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getResourceById_zeroId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.getResourceById("0"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getResourceById_negativeId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.getResourceById("-5"))
                .isInstanceOf(InvalidRequestException.class);
    }

    // --- deleteResources ---

    @Test
    void deleteResources_existingIds_deletesFromStorageDbAndSongService() {
        Resource r1 = new Resource(1, "songs/1_a.mp3");
        Resource r2 = new Resource(2, "songs/2_b.mp3");

        when(repository.existsById(1)).thenReturn(true);
        when(repository.existsById(2)).thenReturn(true);
        when(repository.findById(1)).thenReturn(Optional.of(r1));
        when(repository.findById(2)).thenReturn(Optional.of(r2));

        DeleteResourcesResponseDto result = resourceService.deleteResources("1,2");

        assertThat(result.getIds()).containsExactlyInAnyOrder(1, 2);
        verify(cloudStorageService).deleteFile("songs/1_a.mp3");
        verify(cloudStorageService).deleteFile("songs/2_b.mp3");
        verify(repository).deleteById(1);
        verify(repository).deleteById(2);
        verify(songServiceClient).deleteMetadata(1);
        verify(songServiceClient).deleteMetadata(2);
    }

    @Test
    void deleteResources_nonExistentIds_silentlySkippedAndReturnedEmpty() {
        when(repository.existsById(99)).thenReturn(false);

        DeleteResourcesResponseDto result = resourceService.deleteResources("99");

        assertThat(result.getIds()).isEmpty();
        verifyNoInteractions(cloudStorageService, songServiceClient);
    }

    @Test
    void deleteResources_emptyCsv_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.deleteResources(""))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteResources_csvExceedsMaxLength_throwsInvalidRequestException() {
        String longCsv = "1,".repeat(100) + "1";
        assertThatThrownBy(() -> resourceService.deleteResources(longCsv))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("200");
    }

    @Test
    void deleteResources_nonNumericId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> resourceService.deleteResources("1,bad"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteResources_withSpacesAroundIds_parsesCorrectly() {
        Resource r1 = new Resource(1, "songs/1_a.mp3");
        when(repository.existsById(1)).thenReturn(true);
        when(repository.findById(1)).thenReturn(Optional.of(r1));

        DeleteResourcesResponseDto result = resourceService.deleteResources(" 1 ");

        assertThat(result.getIds()).containsExactly(1);
    }
}
