package com.cotalk.infrastructure.config.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 애플리케이션 공통 설정 프로퍼티.
 *
 * @param frontendUrl   프론트엔드 URL
 * @param cors          CORS 설정
 * @param redis         Redis 채널 설정
 * @param passwordReset 비밀번호 재설정 설정
 * @param terms         약관 버전 설정
 * @param encryption    암호화 설정
 * @param swagger       Swagger UI 설정
 * @param search        메시지 검색(블라인드 인덱스) 설정
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Cors cors,
        Redis redis,
        PasswordReset passwordReset,
        Terms terms,
        Encryption encryption,
        Swagger swagger,
        Search search
) {
    private static final Logger log = LoggerFactory.getLogger(AppProperties.class);
    /**
     * CORS 설정.
     *
     * @param allowedOrigins 허용된 오리진 목록 (쉼표 구분)
     */
    public record Cors(String allowedOrigins) {
        public Cors {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                allowedOrigins = "http://localhost:3000";
                log.warn("CORS allowedOrigins가 설정되지 않아 localhost 기본값을 사용합니다. 프로덕션 환경에서는 app.cors.allowed-origins를 반드시 설정하세요.");
            } else if (allowedOrigins.contains("localhost")) {
                log.warn("CORS allowedOrigins에 localhost가 포함되어 있습니다. 프로덕션 환경에서는 실제 도메인을 사용하세요.");
            }
        }
    }

    /**
     * Redis 채널 설정.
     *
     * @param channelPrefix   채팅 채널 접두사
     * @param userEventPrefix 사용자 이벤트 채널 접두사
     */
    public record Redis(String channelPrefix, String userEventPrefix) {
        public Redis {
            if (channelPrefix == null || channelPrefix.isBlank()) {
                channelPrefix = "chat:room:";
            }
            if (userEventPrefix == null || userEventPrefix.isBlank()) {
                userEventPrefix = "user:event:";
            }
        }
    }

    /**
     * 비밀번호 재설정 설정.
     *
     * @param expirationMinutes 토큰 만료 시간 (분)
     */
    public record PasswordReset(int expirationMinutes) {
        public PasswordReset {
            if (expirationMinutes <= 0) {
                expirationMinutes = 30;
            }
        }
    }

    /**
     * 약관 버전 설정.
     *
     * @param serviceVersion 서비스 이용약관 버전
     * @param privacyVersion 개인정보처리방침 버전
     */
    public record Terms(String serviceVersion, String privacyVersion) {
        public Terms {
            if (serviceVersion == null || serviceVersion.isBlank()) {
                serviceVersion = "1.0";
            }
            if (privacyVersion == null || privacyVersion.isBlank()) {
                privacyVersion = "1.0";
            }
        }
    }

    /**
     * 암호화 설정.
     *
     * @param key     AES-256 암호화 키 (Base64 인코딩)
     * @param enabled 암호화 활성화 여부
     */
    public record Encryption(String key, boolean enabled) {
        public Encryption {
            if (key == null) {
                key = "";
            }
        }
    }

    /**
     * 메시지 검색(블라인드 인덱스) 설정.
     *
     * <p>{@code blindIndexSecret}은 HMAC-SHA256 트라이그램 토큰 생성용 시크릿(Base64)이며,
     * AES 암호화 키({@link Encryption#key()})와 완전히 분리된 별도 시크릿이다.
     * 프로덕션에서는 반드시 환경변수로 주입해야 하며 기본값이 없다(보안).</p>
     *
     * @param blindIndexSecret 블라인드 인덱스 HMAC 시크릿 (Base64, 기본값 없음)
     */
    public record Search(String blindIndexSecret) {
        public Search {
            if (blindIndexSecret == null) {
                blindIndexSecret = "";
            }
        }
    }

    /**
     * Swagger UI 설정.
     *
     * @param serverUrl         서버 URL
     * @param serverDescription 서버 설명
     */
    public record Swagger(String serverUrl, String serverDescription) {
        public Swagger {
            if (serverUrl == null || serverUrl.isBlank()) {
                serverUrl = "http://localhost:8080";
                log.warn("Swagger serverUrl이 설정되지 않아 localhost 기본값을 사용합니다. 프로덕션 환경에서는 app.swagger.server-url을 반드시 설정하세요.");
            } else if (serverUrl.contains("localhost")) {
                log.warn("Swagger serverUrl에 localhost가 포함되어 있습니다. 프로덕션 환경에서는 실제 도메인을 사용하세요.");
            }
            if (serverDescription == null || serverDescription.isBlank()) {
                serverDescription = "API 서버";
            }
        }
    }

    public AppProperties {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "http://localhost:3000";
            log.warn("frontendUrl이 설정되지 않아 localhost 기본값을 사용합니다. 프로덕션 환경에서는 app.frontend-url을 반드시 설정하세요.");
        } else if (frontendUrl.contains("localhost")) {
            log.warn("frontendUrl에 localhost가 포함되어 있습니다. 프로덕션 환경에서는 실제 도메인을 사용하세요.");
        }
        if (cors == null) {
            cors = new Cors("http://localhost:3000");
        }
        if (redis == null) {
            redis = new Redis("chat:room:", "user:event:");
        }
        if (passwordReset == null) {
            passwordReset = new PasswordReset(30);
        }
        if (terms == null) {
            terms = new Terms("1.0", "1.0");
        }
        if (encryption == null) {
            encryption = new Encryption("", true);
        }
        if (swagger == null) {
            swagger = new Swagger("http://localhost:8080", "API 서버");
        }
        if (search == null) {
            search = new Search("");
        }
    }
}
