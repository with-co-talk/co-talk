package com.cotalk.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * MinIO 연동을 위한 스프링 설정 클래스.
 * AWS S3 SDK를 사용하여 MinIO 서버와 통신하기 위한 클라이언트들을 구성한다.
 *
 * <p>MinIO가 활성화되었을 때({@code minio.enabled=true}) 자동으로 활성화된다.
 *
 * <p>필요한 설정 프로퍼티:
 * <ul>
 *   <li>{@code minio.endpoint} - MinIO 서버 엔드포인트 URL</li>
 *   <li>{@code minio.access-key} - 접근 키</li>
 *   <li>{@code minio.secret-key} - 비밀 키</li>
 *   <li>{@code minio.region} - 리전 (기본값: us-east-1)</li>
 * </ul>
 *
 * @author seunggu.lee
 * @see MinioFileStorage
 */
@Configuration
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    /**
     * MinIO와 통신하기 위한 S3Client 빈을 생성한다.
     *
     * <p>MinIO는 path-style 접근 방식을 요구하므로 {@code forcePathStyle(true)}가 설정된다.
     *
     * @return 구성된 S3Client 인스턴스
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true) // MinIO requires path-style access
                .build();
    }

    /**
     * Pre-signed URL 생성을 위한 S3Presigner 빈을 생성한다.
     *
     * @return 구성된 S3Presigner 인스턴스
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}
