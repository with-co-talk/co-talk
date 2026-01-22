package com.cotalk.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MinioConfig 테스트.
 * minio.enabled=true일 때 S3Client와 S3Presigner 빈이 생성되는지 확인한다.
 *
 * @author seunggu.lee
 */
@SpringBootTest(classes = MinioConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "minio.enabled=true",
        "minio.endpoint=http://localhost:9000",
        "minio.access-key=minioadmin",
        "minio.secret-key=minioadmin",
        "minio.region=us-east-1"
})
@DisplayName("MinioConfig")
class MinioConfigTest {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Test
    @DisplayName("S3Client 빈이 생성된다")
    void should_createS3Client_when_minioEnabled() {
        assertThat(s3Client).isNotNull();
    }

    @Test
    @DisplayName("S3Presigner 빈이 생성된다")
    void should_createS3Presigner_when_minioEnabled() {
        assertThat(s3Presigner).isNotNull();
    }
}
