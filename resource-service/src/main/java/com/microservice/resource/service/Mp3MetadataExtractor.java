package com.microservice.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for extracting MP3 metadata.
 * Uses Apache Tika to parse MP3 files and extract metadata.
 */
@Slf4j
@Component
public class Mp3MetadataExtractor {

    private final Metadata metadata;
    private final ContentHandler handler;
    private final ParseContext context;
    private final Mp3Parser parser;

    public Mp3MetadataExtractor() {
        this.metadata = new Metadata();
        this.handler = new DefaultHandler();
        this.context = new ParseContext();
        this.parser = new Mp3Parser();
    }

    /**
     * Extracts metadata from MP3 audio data.
     * Uses Apache Tika to parse MP3 files.
     *
     * @param audioData Binary MP3 data.
     * @return Map of extracted metadata.
     */
    public Map<String, String> extractMetadata(long id, byte[] audioData) {
        Map<String, String> extractedMetadata = new HashMap<>();

        try {
            // Parse MP3 file using Tika
            ByteArrayInputStream stream = new ByteArrayInputStream(audioData);
            parser.parse(stream, handler, metadata, context);

            // Extract common metadata fields
            extractedMetadata.put("id", String.valueOf(id));
            extractedMetadata.put("name", getMetadataValue(metadata, "dc:title", "Unknown"));
            extractedMetadata.put("artist", getMetadataValue(metadata, "xmpDM:artist", "Unknown"));
            extractedMetadata.put("album", getMetadataValue(metadata, "xmpDM:album", "Unknown"));
            extractedMetadata.put("duration", convertDuration(getMetadataValue(metadata, "xmpDM:duration", "0")));
            extractedMetadata.put("year", getMetadataValue(metadata, "xmpDM:releaseDate", "Unknown"));

            log.info("Metadata extracted successfully from MP3 file using Apache Tika");
            return extractedMetadata;
        } catch (Exception e) {
            log.error("Failed to extract metadata from MP3 file: {}", e.getMessage());
            // Return empty metadata map on failure instead of throwing exception
            return new HashMap<>();
        }
    }

    /**
     * Gets metadata value from MP3 audio data by certain key
     * @param audioData Binary MP3 data.
     * @param key Metadata key.
     * @param defaultValue Default value if key not found.
     * @return Metadata value or default.
     */
    public String getMetadataValue(byte[] audioData, String key, String defaultValue) {
        ByteArrayInputStream stream = new ByteArrayInputStream(audioData);
        try {
            parser.parse(stream, handler, metadata, context);
        } catch (Exception e) {
            log.error("Failed to extract metadata {} from MP3 file: {}", key, e.getMessage());
            return defaultValue;
        }
        return getMetadataValue(metadata, key, defaultValue);
    }

    /**
     * Gets metadata value from Tika Metadata object with fallback.
     *
     * @param metadata     Tika Metadata object.
     * @param key          Metadata key.
     * @param defaultValue Default value if key not found.
     * @return Metadata value or default.
     */
    private String getMetadataValue(Metadata metadata, String key, String defaultValue) {
        String value = metadata.get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Converts duration from seconds to MM:SS, with leading zeros.
     *
     * @param durationSeconds Duration in seconds as a string.
     * @return Formatted duration string or null if input is invalid.
     */
    private String convertDuration(String durationSeconds) {
        if (durationSeconds == null) return null;

        try {
            double secondsDouble = Double.parseDouble(durationSeconds);
            int totalSeconds = (int) secondsDouble;

            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            return String.format("%02d:%02d", minutes, seconds);

        } catch (NumberFormatException e) {
            return null;
        }
    }
}


