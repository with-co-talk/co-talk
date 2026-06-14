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
 * @param lock          분산락 설정
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
        Search search,
        Lock lock
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
     * @param backfill         기존 메시지 검색 토큰 백필(1회성 관리 작업) 설정
     */
    public record Search(String blindIndexSecret, Backfill backfill) {
        public Search {
            if (blindIndexSecret == null) {
                blindIndexSecret = "";
            }
            if (backfill == null) {
                backfill = new Backfill(false, 0, 0L, true);
            }
        }

        /**
         * 백필 설정을 생략하고 시크릿만으로 {@link Search}를 만든다(백필 기본 비활성).
         *
         * <p>{@code @ConfigurationProperties} 생성자 바인딩이 모호해지지 않도록(단일 생성자 유지)
         * 별도 생성자가 아닌 정적 팩터리로 제공한다. 테스트/수동 생성용.</p>
         *
         * @param blindIndexSecret 블라인드 인덱스 HMAC 시크릿
         * @return 백필 기본값(비활성)을 가진 {@link Search}
         */
        public static Search of(String blindIndexSecret) {
            return new Search(blindIndexSecret, null);
        }
    }

    /**
     * 기존 암호화 메시지 검색 토큰 백필(PR2) 설정.
     *
     * <p>대량 데이터를 복호화→토큰화→적재하는 1회성 관리 작업이라 실수 실행을 막기 위해
     * 기본 비활성({@code enabled=false})이다. 운영에서는 {@code app.search.backfill.enabled=true}로
     * 명시적으로 켜고, 청크 크기/throttle로 부하를 제어한다.</p>
     *
     * @param enabled        애플리케이션 기동 시 백필 자동 실행 여부 (기본 false — 안전)
     * @param chunkSize      한 청크(=한 트랜잭션) 당 메시지 수 (0/음수면 서비스 기본 500)
     * @param throttleMillis 청크 사이 슬립(ms). 운영 부하 제어용 (기본 0 = 슬립 없음)
     * @param skipExisting   이미 토큰이 있는 메시지(신규 PR1 적재분)는 건너뛸지 여부 (기본 true)
     */
    public record Backfill(boolean enabled, int chunkSize, long throttleMillis, boolean skipExisting) {
    }

    /**
     * 분산락 설정.
     *
     * <p>{@code failClosed}는 Redisson(RedissonClient)이 없어 분산락이 NoOp(락 없이 즉시 실행)로
     * 강등되는 상황을 다루는 정책 스위치다.</p>
     * <ul>
     *   <li>{@code false}(기본): 하위호환. NoOp 진입 시 경고만 남기고 락 없이 작업을 실행한다.
     *       기존 기동 동작(Redis 미가용 시에도 앱 정상 동작)을 보존한다.</li>
     *   <li>{@code true}(운영 권장): fail-closed. NoOp 상태에서 락 보호가 필요한 작업을 실행하면
     *       {@code DistributedLockException}을 던져 동시성 보호가 조용히 사라지는 것을 차단한다.</li>
     * </ul>
     *
     * <p>기본값을 {@code false}로 둔 이유: 기존 테스트/로컬/CI 환경(Redis 없이 기동 후 NoOp 동작에
     * 의존)을 깨지 않기 위함이다. 운영에서는 {@code app.lock.fail-closed=true}로 명시적으로 켠다.</p>
     *
     * @param failClosed NoOp 강등 시 예외를 던질지 여부 (기본 false — 하위호환)
     */
    public record Lock(boolean failClosed) {
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
            search = new Search("", null);
        }
        if (lock == null) {
            lock = new Lock(false);
        }
    }
}
