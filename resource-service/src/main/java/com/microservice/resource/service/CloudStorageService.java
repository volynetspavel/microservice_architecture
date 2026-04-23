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

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    public CloudStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads an MP3 file to cloud storage.
     * @param fileLocation The location where the file will be stored in the bucket.
     * @param audioData The binary data of the MP3 file.
     */
    public void uploadAudioFile(String fileLocation, byte[] audioData) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileLocation)
                .contentType("audio/mpeg")
                .build();

        s3Client.putObject(request,
                software.amazon.awssdk.core.sync.RequestBody.fromByteBuffer(
                        java.nio.ByteBuffer.wrap(audioData)));
    }

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

    public void deleteFile(String fileLocation) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileLocation)
                .build();

        s3Client.deleteObject(request);
    }

    public String getFileUrl(String fileLocation) {
        return endpoint + "/" + bucketName + "/" + fileLocation;
    }
}
