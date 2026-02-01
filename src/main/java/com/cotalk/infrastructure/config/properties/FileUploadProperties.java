package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 파일 업로드 설정 프로퍼티.
 *
 * @param maxSize 최대 파일 크기 (바이트)
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "file.upload")
public record FileUploadProperties(
        long maxSize
) {
    public FileUploadProperties {
        if (maxSize <= 0) {
            maxSize = 10485760; // 10MB
        }
    }
}
