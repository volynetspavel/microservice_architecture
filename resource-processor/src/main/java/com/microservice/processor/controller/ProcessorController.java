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


}
