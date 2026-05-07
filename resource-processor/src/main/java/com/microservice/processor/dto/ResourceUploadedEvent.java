package com.microservice.processor.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing an event that is published when a new resource is uploaded.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResourceUploadedEvent {
    private int resourceId;
}