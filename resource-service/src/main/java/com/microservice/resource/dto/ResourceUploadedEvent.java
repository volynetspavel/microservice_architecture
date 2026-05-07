package com.microservice.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO representing an event that is published when a new resource is uploaded.
 * Contains the ID of the uploaded resource.
 * {@code @NoArgsConstructor} is required for JSON deserialization on the consumer side.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadedEvent {
    private int resourceId;
}