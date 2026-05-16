package com.microservice.processor.service;

import com.microservice.processor.dto.SongCreateRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class Mp3MetadataExtractorTest {

    private Mp3MetadataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new Mp3MetadataExtractor();
    }

    @Test
    void extractMetadata_invalidBytes_returnsDefaultValues() {
        SongCreateRequestDto result = extractor.extractMetadata(42, new byte[0]);

        assertThat(result.id()).isEqualTo(42);
        assertThat(result.name()).isEqualTo("Unknown");
        assertThat(result.artist()).isEqualTo("Unknown");
        assertThat(result.album()).isEqualTo("Unknown");
        assertThat(result.year()).isEqualTo("0000");
    }

    @Test
    void extractMetadata_resourceIdIsPreservedInResult() {
        SongCreateRequestDto result = extractor.extractMetadata(99, new byte[0]);

        assertThat(result.id()).isEqualTo(99);
    }
}
