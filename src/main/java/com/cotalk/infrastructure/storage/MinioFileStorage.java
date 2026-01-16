package com.cotalk.infrastructure.storage;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.outbound.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicUrl;

    public MinioFileStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${minio.bucket}") String bucketName,
            @Value("${minio.public-url:}") String publicUrl) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
        
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            log.info("Bucket '{}' already exists", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("Creating bucket '{}'", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    @Override
    public String upload(InputStream inputStream, String fileName, String contentType, long fileSize) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, fileSize));

            log.info("File uploaded successfully: {}", fileName);

            // Return public URL or generate one
            if (publicUrl != null && !publicUrl.isEmpty()) {
                return publicUrl + "/" + bucketName + "/" + fileName;
            }
            return generatePresignedUrl(fileName, 60 * 24 * 7); // 7 days default
        } catch (Exception e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw FileUploadException.uploadFailed(e);
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String fileName, int expirationMinutes) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
