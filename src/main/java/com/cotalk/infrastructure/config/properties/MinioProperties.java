package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 파일 저장소 설정 프로퍼티.
 *
 * @param enabled          MinIO 활성화 여부
 * @param endpoint         MinIO 서버 엔드포인트 URL
 * @param accessKey        액세스 키
 * @param secretKey        시크릿 키
 * @param bucket           버킷 이름
 * @param publicUrl        외부 접근용 공개 URL
 * @param region           리전 (기본값: us-east-1)
 * @param publicReadPolicy 버킷 공개 읽기 정책 설정 여부.
 *                         {@code true}이면 버킷에 공개 읽기 정책을 적용하여 인증 없이 파일에 접근 가능하다.
 *                         {@code false}이면 공개 읽기 정책을 적용하지 않으며,
 *                         파일 접근 시 Pre-signed URL이 필요하다. (기본값: true)
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        boolean enabled,
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicUrl,
        String region,
        boolean publicReadPolicy
) {
    public MinioProperties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "cotalk";
        }
    }
}
