package com.microservice.processor.dto;

public record SongCreateRequestDto(int id, String name, String artist, String album, String duration, String year) {
}
