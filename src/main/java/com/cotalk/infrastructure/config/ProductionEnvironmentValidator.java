package com.cotalk.infrastructure.config;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.config.properties.MinioProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로덕션 환경에서 필수 운영 설정을 검증한다.
 *
 * <p>로컬 개발용 기본값(localhost, 인메모리 저장소 등)으로 서버가 기동되는 것을 방지한다.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionEnvironmentValidator implements ApplicationRunner {

    private final AppProperties appProperties;
    private final MinioProperties minioProperties;

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        List<String> violations = new ArrayList<>();

        requireNonLocalUrl("app.frontend-url", appProperties.frontendUrl(), violations);
        requireNonLocalOrigins("app.cors.allowed-origins", appProperties.cors().allowedOrigins(), violations);

        if (appProperties.encryption().enabled() && isBlank(appProperties.encryption().key())) {
            violations.add("app.encryption.key must be set when encryption is enabled");
        }

        if (!minioProperties.enabled()) {
            violations.add("minio.enabled must be true in production");
        } else {
            requireNonLocalUrl("minio.endpoint", minioProperties.endpoint(), violations);
            requireNonLocalUrl("minio.public-url", minioProperties.publicUrl(), violations);

            if (isBlank(minioProperties.accessKey())) {
                violations.add("minio.access-key must be set in production");
            }
            if (isBlank(minioProperties.secretKey())) {
                violations.add("minio.secret-key must be set in production");
            }
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException("Invalid production configuration: " + String.join("; ", violations));
        }
    }

    private void requireNonLocalOrigins(String propertyName, String origins, List<String> violations) {
        if (isBlank(origins)) {
            violations.add(propertyName + " must be set in production");
            return;
        }

        for (String origin : origins.split(",")) {
            String trimmedOrigin = origin.trim();
            if (trimmedOrigin.isEmpty()) {
                continue;
            }
            if (isLocalUrl(trimmedOrigin)) {
                violations.add(propertyName + " contains a local-only origin: " + trimmedOrigin);
            }
        }
    }

    private void requireNonLocalUrl(String propertyName, String value, List<String> violations) {
        if (isBlank(value)) {
            violations.add(propertyName + " must be set in production");
            return;
        }

        if (isLocalUrl(value)) {
            violations.add(propertyName + " must not point to localhost in production: " + value);
        }
    }

    private boolean isLocalUrl(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            String normalizedHost = host.toLowerCase();
            return normalizedHost.equals("localhost")
                    || normalizedHost.equals("127.0.0.1")
                    || normalizedHost.equals("::1")
                    || normalizedHost.endsWith(".local");
        } catch (URISyntaxException e) {
            return value.toLowerCase().contains("localhost");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
