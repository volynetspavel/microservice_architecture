package com.microservice.processor.service;

import com.microservice.processor.dto.AudioMetadataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for processing audio files and extracting metadata.
 */
@Slf4j
@Service
public class ProcessorService {
    private final Mp3MetadataExtractor mp3MetadataExtractor;

    public ProcessorService(Mp3MetadataExtractor mp3MetadataExtractor) {
        this.mp3MetadataExtractor = mp3MetadataExtractor;
    }

    /**
     * Processes audio file and extracts metadata.
     *
     * @param id        Resource ID
     * @param audioData Binary audio data (MP3)
     * @return AudioMetadataDto with extracted metadata
     */
    public AudioMetadataDto processResource(long id, byte[] audioData) {
        Map<String, String> metadata = mp3MetadataExtractor.extractMetadata(id, audioData);
        return new AudioMetadataDto(metadata);
    }
}
