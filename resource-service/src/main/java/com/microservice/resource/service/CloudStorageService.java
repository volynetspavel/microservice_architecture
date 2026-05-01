package com.microservice.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

/**
 * Service for handling interactions with cloud storage (e.g., AWS S3).
 * Provides methods to upload, download, and delete MP3 files in the cloud.
 */
@Service
public class CloudStorageService {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final String songBucketFolder = "songs/";

    public CloudStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads an MP3 file to cloud storage.
     *
     * @param fileName  The name of the file to be stored in the bucket.
     * @param audioData The binary data of the MP3 file.
     * @return The URL of the uploaded file.
     */
    public String uploadAudioFile(String fileName, byte[] audioData) {
        String fileLocation = songBucketFolder + fileName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileLocation)
                .contentType("audio/mpeg")
                .build();

        s3Client.putObject(request,
                software.amazon.awssdk.core.sync.RequestBody.fromByteBuffer(
                        java.nio.ByteBuffer.wrap(audioData)));
        return fileLocation;
    }

    /**
     * Downloads an MP3 file from cloud storage.
     *
     * @param fileLocation The location of the file in the bucket.
     * @return The binary data of the MP3 file.
     */
    public byte[] downloadFile(String fileLocation) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileLocation)
                .build();

        try {
            return s3Client.getObject(request).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes an MP3 file from cloud storage.
     *
     * @param fileLocation The location of the file in the bucket to be deleted.
     */
    public void deleteFile(String fileLocation) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileLocation)
                .build();

        s3Client.deleteObject(request);
    }
}
