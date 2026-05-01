package com.microservice.resource.service;

import com.microservice.resource.dto.DeleteResourcesResponseDto;
import com.microservice.resource.dto.ResourceDataResponseDto;
import com.microservice.resource.dto.ResourceIdResponseDto;
import com.microservice.resource.entity.Resource;
import com.microservice.resource.exception.InvalidRequestException;
import com.microservice.resource.exception.ResourceNotFoundException;
import com.microservice.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling MP3 resource CRUD operations.
 */
@Service
public class ResourceService {

    private final ResourceRepository repository;
    private final Mp3MetadataExtractor metadataExtractor;
    private final SongServiceClient songServiceClient;
    private final CloudStorageService cloudStorageService;

    public ResourceService(ResourceRepository repository,
                           Mp3MetadataExtractor metadataExtractor,
                           SongServiceClient songServiceClient,
                           CloudStorageService cloudStorageService) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
        this.songServiceClient = songServiceClient;
        this.cloudStorageService = cloudStorageService;
    }

    /**
     * Uploads an MP3 file, extracts metadata, and stores it.
     *
     * @param audioData Binary MP3 data.
     * @return DTO containing the ID of the created resource.
     */
    public ResourceIdResponseDto uploadResource(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new InvalidRequestException("MP3 file is empty");
        }

        //save resource to database to get generated ID
        Resource resource = repository.save(new Resource(""));

        // Extract metadata from MP3 file
        String title = metadataExtractor.getMetadataValue(audioData, "dc:title", "Unknown");
        String fileName = resource.getId() + "_" + title + ".mp3";

        String fileLocation = cloudStorageService.uploadAudioFile(fileName, audioData);
        // Save resource to database
        resource.setFileLocation(fileLocation);
        // Update resource with correct file location
        repository.save(resource);

        return new ResourceIdResponseDto(resource.getId());
    }

    /**
     * Retrieves the audio data for a resource.
     *
     * @param id Resource ID.
     * @return Binary MP3 data.
     */
    public ResourceDataResponseDto getResourceById(String id) {
        int validatedId = validateResourceId(id);
        Resource resource = repository.findById(validatedId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));

        byte[] audioData = cloudStorageService.downloadFile(resource.getFileLocation());
        return new ResourceDataResponseDto(audioData);
    }

    /**
     * Deletes resources by IDs.
     *
     * @param resourceIds Comma-separated string of resource IDs to delete.
     * @return DTO containing the IDs of successfully deleted resources.
     */
    public DeleteResourcesResponseDto deleteResources(String resourceIds) {
        validateCsvLength(resourceIds);
        List<Integer> ids = parseCsvIds(resourceIds);

        List<Integer> deletedIds = new ArrayList<>();
        for (Integer id : ids) {
            if (repository.existsById(id)) {
                Resource resource = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));

                cloudStorageService.deleteFile(resource.getFileLocation());
                repository.deleteById(id);
                songServiceClient.deleteMetadata(id);

                deletedIds.add(id);
            }
        }

        return new DeleteResourcesResponseDto(deletedIds);
    }

    /**
     * Validates if the CSV string length is within acceptable limits.
     *
     * @param resourceIds CSV string of IDs.
     * @throws InvalidRequestException if CSV string is too long.
     */
    private void validateCsvLength(String resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            throw new InvalidRequestException("CSV string cannot be empty");
        }
        if (resourceIds.length() > 200) {
            throw new InvalidRequestException("CSV string is too long: received " + resourceIds.length() + " characters, maximum allowed is 200");
        }
    }

    /**
     * Parses comma-separated string of IDs into a list of Longs.
     *
     * @param resourceIds CSV string of IDs.
     * @return List of parsed IDs.
     * @throws InvalidRequestException if IDs cannot be parsed.
     */
    private List<Integer> parseCsvIds(String resourceIds) {
        List<String> stringIds = Arrays.stream(resourceIds.split(","))
                .map(String::trim)
                .toList();

        try {
            return stringIds.stream()
                    .peek(this::validateCsvFormat)
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid IDs in the provided CSV string");
        }
    }

    /**
     * Validates CSV format.
     *
     * @param resourceIds CSV string of IDs.
     * @throws InvalidRequestException if CSV format is invalid.
     */
    private void validateCsvFormat(String resourceIds) {
        if (!resourceIds.matches("^\\d+(?:,\\s*\\d+)*$")) {
            throw new InvalidRequestException("Invalid ID format: '" + resourceIds + "'. Only positive integers are allowed");
        }
    }

    /**
     * Validates if the provided ID is a positive number.
     *
     * @param id Resource ID to validate.
     * @throws InvalidRequestException if ID is invalid.
     */
    private int validateResourceId(String id) {
        int parsedId;
        try {
            parsedId = Integer.parseInt(id);
            if (parsedId <= 0) {
                throw new InvalidRequestException("Invalid value '" + id + "' for ID. Must be a positive integer");
            }
            return parsedId;
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
    }
}