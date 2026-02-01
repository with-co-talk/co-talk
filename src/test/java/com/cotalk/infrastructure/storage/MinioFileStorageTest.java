package com.cotalk.infrastructure.storage;

import com.cotalk.domain.exception.FileStorageException;
import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.infrastructure.config.properties.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * MinioFileStorage 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MinioFileStorage 단위 테스트")
class MinioFileStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String PUBLIC_URL = "http://minio.example.com";

    private MinioFileStorage fileStorage;

    @BeforeEach
    void setUp() {
        // Mock bucket exists check
        given(s3Client.headBucket(any(HeadBucketRequest.class)))
                .willReturn(HeadBucketResponse.builder().build());

        MinioProperties minioProperties = createMinioProperties(BUCKET_NAME, PUBLIC_URL);
        fileStorage = new MinioFileStorage(s3Client, s3Presigner, minioProperties);
    }

    private MinioProperties createMinioProperties(String bucket, String publicUrl) {
        return new MinioProperties(
                true,
                "http://localhost:9000",
                "access-key",
                "secret-key",
                bucket,
                publicUrl,
                "us-east-1"
        );
    }

    @Test
    @DisplayName("버킷이 없을 때 생성")
    void should_createBucket_when_bucketNotExists() {
        // given
        S3Client newS3Client = mock(S3Client.class);
        S3Presigner newPresigner = mock(S3Presigner.class);

        given(newS3Client.headBucket(any(HeadBucketRequest.class)))
                .willThrow(NoSuchBucketException.builder().message("No bucket").build());
        given(newS3Client.createBucket(any(CreateBucketRequest.class)))
                .willReturn(CreateBucketResponse.builder().build());

        // when
        MinioProperties minioProperties = createMinioProperties(BUCKET_NAME, PUBLIC_URL);
        new MinioFileStorage(newS3Client, newPresigner, minioProperties);

        // then
        verify(newS3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 공개 URL 사용")
    void should_returnPublicUrl_when_uploadWithPublicUrl() {
        // given
        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        String url = fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // then
        assertEquals(PUBLIC_URL + "/" + BUCKET_NAME + "/" + fileName, url);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 공개 URL 없이 Pre-signed URL 반환")
    void should_returnPresignedUrl_when_noPublicUrl() throws Exception {
        // given
        given(s3Client.headBucket(any(HeadBucketRequest.class)))
                .willReturn(HeadBucketResponse.builder().build());

        MinioProperties minioPropertiesNoPublicUrl = createMinioProperties(BUCKET_NAME, "");
        MinioFileStorage storageWithoutPublicUrl = new MinioFileStorage(
                s3Client, s3Presigner, minioPropertiesNoPublicUrl);

        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";
        String presignedUrl = "http://presigned-url.example.com/test.txt";

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create(presignedUrl).toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedRequest);

        // when
        String url = storageWithoutPublicUrl.upload(inputStream, fileName, "text/plain", content.length());

        // then
        assertEquals(presignedUrl, url);
    }

    @Test
    @DisplayName("파일 업로드 실패 - 예외 발생")
    void should_throwFileUploadException_when_uploadFails() {
        // given
        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(S3Exception.builder().message("Upload failed").build());

        // when & then
        assertThrows(FileUploadException.class,
                () -> fileStorage.upload(inputStream, fileName, "text/plain", content.length()));
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void should_deleteFile_when_validFileName() {
        // given
        String fileName = "test.txt";
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(DeleteObjectResponse.builder().build());

        // when
        fileStorage.delete(fileName);

        // then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("파일 삭제 실패 - 예외 발생")
    void should_throwFileStorageException_when_deleteFails() {
        // given
        String fileName = "test.txt";
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(S3Exception.builder().message("Delete failed").build());

        // when & then
        assertThrows(FileStorageException.class, () -> fileStorage.delete(fileName));
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재하는 파일")
    void should_returnTrue_when_fileExists() {
        // given
        String fileName = "test.txt";
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder().build());

        // when
        boolean exists = fileStorage.exists(fileName);

        // then
        assertTrue(exists);
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재하지 않는 파일")
    void should_returnFalse_when_fileNotExists() {
        // given
        String fileName = "test.txt";
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willThrow(NoSuchKeyException.builder().message("No such key").build());

        // when
        boolean exists = fileStorage.exists(fileName);

        // then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Pre-signed URL 생성 성공")
    void should_generatePresignedUrl_when_validFileName() throws Exception {
        // given
        String fileName = "test.txt";
        int expirationMinutes = 30;
        String presignedUrl = "http://presigned-url.example.com/test.txt?signed=xyz";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create(presignedUrl).toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedRequest);

        // when
        String url = fileStorage.generatePresignedUrl(fileName, expirationMinutes);

        // then
        assertEquals(presignedUrl, url);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("업로드 시 올바른 content-type 전달")
    void should_passCorrectContentType_when_upload() {
        // given
        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String fileName = "image.png";
        String contentType = "image/png";

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        fileStorage.upload(inputStream, fileName, contentType, content.length());

        // then
        verify(s3Client).putObject(
                argThat((PutObjectRequest request) ->
                        request.contentType().equals(contentType) &&
                        request.key().equals(fileName) &&
                        request.bucket().equals(BUCKET_NAME)),
                any(RequestBody.class));
    }
}
