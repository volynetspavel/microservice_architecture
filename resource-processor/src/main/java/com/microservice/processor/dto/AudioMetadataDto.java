package com.microservice.processor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for responding with audio metadata extracted from the uploaded audio file.
 */
@Getter
@Setter
public class AudioMetadataDto {
    @JsonProperty("metadata")
    private Map<String, String> metadata;

    public AudioMetadataDto(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
