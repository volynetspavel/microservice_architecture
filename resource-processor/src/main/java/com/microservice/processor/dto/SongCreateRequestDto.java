package com.microservice.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO for creating a new song resource.
 * This DTO is used to receive song details from the client when creating a new song resource.
 */
@Getter
@AllArgsConstructor
public class SongCreateRequestDto {
    private int id;
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;
}