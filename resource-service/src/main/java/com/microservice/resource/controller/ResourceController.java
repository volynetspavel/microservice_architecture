package com.microservice.resource.controller;

import com.microservice.resource.dto.DeleteResourcesResponseDto;
import com.microservice.resource.dto.ResourceDataResponseDto;
import com.microservice.resource.dto.ResourceIdResponseDto;
import com.microservice.resource.service.ResourceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing MP3 resources.
 * Handles HTTP requests for resource upload, retrieval, and deletion.
 */
@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Uploads a new MP3 resource.
     *
     * @param audioFile The MP3 file to be uploaded
     * @return ResponseEntity with resource ID and 200 OK status
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResourceIdResponseDto> uploadResource(@RequestParam("audioFile") MultipartFile audioFile) {
        ResourceIdResponseDto resourceId = resourceService.uploadResource(audioFile);
        return ResponseEntity.status(HttpStatus.OK).body(resourceId);
    }

    /**
     * Retrieves the binary audio data of a resource.
     *
     * @param id The ID of the resource to retrieve
     * @return ResponseEntity with audio bytes and appropriate status code
     */
    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getResourceById(@PathVariable String id) {
        ResourceDataResponseDto data = resourceService.getResourceById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resource_" + id + ".mp3\"")
                .body(data.getAudioData());
    }

    /**
     * Deletes specified resources by their IDs.
     *
     * @param id Comma-separated list of resource IDs to remove
     * @return ResponseEntity with IDs of deleted resources and 200 OK status
     */
    @DeleteMapping
    public ResponseEntity<DeleteResourcesResponseDto> deleteResources(@RequestParam String id) {
        DeleteResourcesResponseDto deletedResources = resourceService.deleteResources(id);
        return ResponseEntity.ok().body(deletedResources);
    }
}