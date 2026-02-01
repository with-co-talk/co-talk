package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase 설정 프로퍼티.
 *
 * @param enabled         Firebase 활성화 여부
 * @param credentialsPath 서비스 계정 JSON 파일 경로
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        boolean enabled,
        String credentialsPath
) {
    public FirebaseProperties {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            credentialsPath = "firebase-service-account.json";
        }
    }
}
