package com.cotalk.infrastructure.config;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.config.properties.MinioProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionEnvironmentValidatorTest {

    private static final String TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            "12345678901234567890123456789012".getBytes()
    );

    @Test
    @DisplayName("프로덕션 설정이 유효하면 검증을 통과한다")
    void should_passValidation_when_configurationIsProductionReady() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                createAppProperties(
                        "https://app.example.com",
                        "https://app.example.com,https://admin.example.com",
                        TEST_ENCRYPTION_KEY
                ),
                createMinioProperties(
                        true,
                        "https://minio.internal.example.com",
                        "https://cdn.example.com",
                        "access-key",
                        "secret-key"
                )
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("localhost 기반 설정이면 프로덕션 검증에서 실패한다")
    void should_failValidation_when_localhostConfigurationIsUsed() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                createAppProperties(
                        "http://localhost:3000",
                        "http://localhost:3000",
                        TEST_ENCRYPTION_KEY
                ),
                createMinioProperties(
                        true,
                        "http://localhost:9000",
                        "http://localhost:9000",
                        "access-key",
                        "secret-key"
                )
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.frontend-url")
                .hasMessageContaining("app.cors.allowed-origins")
                .hasMessageContaining("minio.endpoint")
                .hasMessageContaining("minio.public-url");
    }

    @Test
    @DisplayName("암호화 키 또는 MinIO 자격증명이 없으면 프로덕션 검증에서 실패한다")
    void should_failValidation_when_requiredSecretsAreMissing() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                createAppProperties(
                        "https://app.example.com",
                        "https://app.example.com",
                        ""
                ),
                createMinioProperties(
                        true,
                        "https://minio.internal.example.com",
                        "https://cdn.example.com",
                        "",
                        ""
                )
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.encryption.key")
                .hasMessageContaining("minio.access-key")
                .hasMessageContaining("minio.secret-key");
    }

    @Test
    @DisplayName("프로덕션에서 인메모리 파일 저장소로 떨어지면 검증에서 실패한다")
    void should_failValidation_when_minioIsDisabled() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                createAppProperties(
                        "https://app.example.com",
                        "https://app.example.com",
                        TEST_ENCRYPTION_KEY
                ),
                createMinioProperties(
                        false,
                        "https://minio.internal.example.com",
                        "https://cdn.example.com",
                        "access-key",
                        "secret-key"
                )
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minio.enabled must be true");
    }

    private AppProperties createAppProperties(String frontendUrl, String allowedOrigins, String encryptionKey) {
        return new AppProperties(
                frontendUrl,
                new AppProperties.Cors(allowedOrigins),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption(encryptionKey, true),
                new AppProperties.Swagger("https://api.example.com", "API 서버")
        );
    }

    private MinioProperties createMinioProperties(
            boolean enabled,
            String endpoint,
            String publicUrl,
            String accessKey,
            String secretKey
    ) {
        return new MinioProperties(enabled, endpoint, accessKey, secretKey, "cotalk", publicUrl, "us-east-1", true);
    }
}
