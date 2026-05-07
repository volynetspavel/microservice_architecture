package com.microservice.processor.service;

import com.microservice.processor.dto.SongCreateRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;

/**
 * Extracts metadata from MP3 audio data using Apache Tika's Mp3Parser.
 */
@Slf4j
@Component
public class Mp3MetadataExtractor {

    private static final String KEY_TITLE = "dc:title";
    private static final String KEY_ARTIST = "xmpDM:artist";
    private static final String KEY_ALBUM = "xmpDM:album";
    private static final String KEY_DURATION = "xmpDM:duration";
    private static final String KEY_RELEASE_DATE = "xmpDM:releaseDate";

    private static final String UNKNOWN = "Unknown";
    private static final String DURATION_FORMAT = "%02d:%02d";
    private static final String DEFAULT_YEAR = "0000";
    private static final String YEAR_REGEX = "^\\d{4}$";

    private final Mp3Parser parser = new Mp3Parser();

    /**
     * Extracts metadata from the given audio data and constructs a SongCreateRequestDto.
     *
     * @param resourceId The ID of the resource associated with the audio data, used to link metadata to the correct song record.
     * @param audioData  The raw byte array of the MP3 audio file from which to extract metadata.
     * @return A SongCreateRequestDto containing the extracted metadata, with default values for any missing fields.
     */
    public SongCreateRequestDto extractMetadata(int resourceId, byte[] audioData) {
        Metadata metadata = new Metadata();
        ContentHandler handler = new DefaultHandler();
        ParseContext context = new ParseContext();

        try {
            parser.parse(new ByteArrayInputStream(audioData), handler, metadata, context);
            log.info("Metadata extracted successfully for resource id {}", resourceId);
        } catch (Exception e) {
            log.error("Failed to extract metadata for resource id {}: {}", resourceId, e.getMessage());
        }

        return new SongCreateRequestDto(
                resourceId,
                getValue(metadata, KEY_TITLE, UNKNOWN),
                getValue(metadata, KEY_ARTIST, UNKNOWN),
                getValue(metadata, KEY_ALBUM, UNKNOWN),
                convertDuration(getValue(metadata, KEY_DURATION, null)),
                sanitizeYear(getValue(metadata, KEY_RELEASE_DATE, null))
        );
    }

    private String getValue(Metadata metadata, String key, String defaultValue) {
        String value = metadata.get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private String convertDuration(String durationSeconds) {
        if (durationSeconds == null) return null;
        try {
            int totalSeconds = (int) Double.parseDouble(durationSeconds);
            return String.format(DURATION_FORMAT, totalSeconds / 60, totalSeconds % 60);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Song Service requires YYYY format (^\d{4}$)
    private String sanitizeYear(String year) {
        if (year != null && year.matches(YEAR_REGEX)) {
            return year;
        }
        return DEFAULT_YEAR;
    }
}
