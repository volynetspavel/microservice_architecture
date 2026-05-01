package com.microservice.processor.controller;

import com.microservice.processor.dto.AudioMetadataDto;
import com.microservice.processor.service.ProcessorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for processing audio files and extracting metadata.
 */
@RestController
@RequestMapping("/processor")
public class ProcessorController {
    private final ProcessorService resourceService;

    public ProcessorController(ProcessorService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Processes the audio file for the given resource ID and extracts metadata.
     * @param id The ID of the resource
     * @param audioData The binary audio data (MP3)
     * @return ResponseEntity containing the extracted metadatan
     */
    @PostMapping(value = "/{id}", consumes = "audio/mpeg", produces = "application/json")
    public ResponseEntity<AudioMetadataDto> processResource(@PathVariable long id, @RequestBody byte[] audioData) {
        AudioMetadataDto metadata = resourceService.processResource(id, audioData);
        return ResponseEntity.status(HttpStatus.OK).body(metadata);
    }
}
