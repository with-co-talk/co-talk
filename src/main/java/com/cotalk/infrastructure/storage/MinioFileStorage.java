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

/**
 * MinIO 기반 파일 저장소 구현체.
 * {@link FileStorage} 포트를 구현하여 MinIO(S3 호환 오브젝트 스토리지)에 파일을 저장한다.
 *
 * <p>MinIO가 활성화되었을 때({@code minio.enabled=true}) 자동으로 활성화된다.
 * AWS S3 SDK를 사용하여 MinIO와 통신하며, 프로덕션 환경에 적합하다.
 *
 * <p>초기화 시 설정된 버킷이 존재하지 않으면 자동으로 생성한다.
 *
 * @author seunggu.lee
 * @see FileStorage
 * @see InMemoryFileStorage
 * @see MinioConfig
 */
@Component
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicUrl;

    /**
     * MinioFileStorage를 생성한다.
     *
     * @param s3Client    S3 클라이언트
     * @param s3Presigner S3 Pre-signed URL 생성기
     * @param bucketName  사용할 버킷 이름
     * @param publicUrl   공개 URL 기본 경로 (선택사항)
     */
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

    /**
     * 버킷이 존재하는지 확인하고, 없으면 생성한다.
     */
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

    /**
     * 파일을 MinIO에 업로드한다.
     *
     * <p>공개 URL이 설정되어 있으면 해당 URL을 반환하고,
     * 그렇지 않으면 7일 유효기간의 Pre-signed URL을 생성하여 반환한다.
     *
     * @param inputStream 업로드할 파일의 입력 스트림
     * @param fileName    저장될 파일명 (오브젝트 키)
     * @param contentType 파일의 MIME 타입
     * @param fileSize    파일 크기 (바이트)
     * @return 업로드된 파일에 접근할 수 있는 URL
     * @throws FileUploadException 파일 업로드에 실패한 경우
     */
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

    /**
     * MinIO에서 파일을 삭제한다.
     *
     * @param fileName 삭제할 파일명 (오브젝트 키)
     * @throws RuntimeException 파일 삭제에 실패한 경우
     */
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

    /**
     * 파일이 MinIO에 존재하는지 확인한다.
     *
     * @param fileName 확인할 파일명 (오브젝트 키)
     * @return 파일이 존재하면 {@code true}, 그렇지 않으면 {@code false}
     */
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

    /**
     * 파일에 대한 Pre-signed URL을 생성한다.
     * 생성된 URL은 지정된 시간 동안만 유효하다.
     *
     * @param fileName          파일명 (오브젝트 키)
     * @param expirationMinutes URL 만료 시간(분)
     * @return 생성된 Pre-signed URL 문자열
     */
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
