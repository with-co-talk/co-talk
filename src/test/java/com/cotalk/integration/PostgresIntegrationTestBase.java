package com.cotalk.integration;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * prod 동등(PostgreSQL + 실제 Flyway 마이그레이션 + 암호화 ON) 환경에서 도는 통합 테스트의 공통 기반.
 *
 * <p>기존 통합 테스트 다수는 H2 인메모리 + {@code ddl-auto=create-drop} + 암호화 OFF로 돌아
 * prod와 스키마/타입/암호화 동작이 달라 "테스트는 통과하지만 운영에서 깨지는" 괴리를 만들었다
 * (예: OAuth 계정탈취 C-1, 암호화 ON에서만 깨진 메시지 검색). 이 기반 클래스는 블라인드 인덱스
 * 검색 통합테스트가 검증된 패턴(실제 PostgreSQL testcontainer + 실제 Flyway V17 + 암호화 ON +
 * {@code ddl-auto=validate})을 재사용 가능하게 추출해, 통합 테스트가 prod 동등 환경에서 돌도록 한다.</p>
 *
 * <p><b>컨테이너 생명주기 — 싱글톤 패턴:</b> JUnit5 {@code @Testcontainers}/{@code @Container}는
 * 컨테이너 생명주기를 <b>테스트 클래스 단위</b>로 관리한다. 그 패턴을 상속 기반 공유에 쓰면 첫 번째
 * 하위 클래스가 끝날 때 컨테이너가 종료되어 이후 클래스가 {@code ConnectException}으로 깨진다. 그래서
 * 여기서는 Testcontainers 공식 권장인 <b>수동 싱글톤</b> 패턴을 쓴다: static 초기화 블록에서 한 번만
 * 시작하고 명시적으로 멈추지 않는다(Ryuk가 JVM 종료 시 정리). 모든 하위 통합 테스트 클래스가 동일
 * 컨테이너/포트를 공유해 부팅 비용을 한 번만 치르고, 클래스 간 종료-재연결 깨짐이 없다.</p>
 *
 * <p>설계 노트:
 * <ul>
 *   <li>{@code spring.jpa.hibernate.ddl-auto=validate}로 Hibernate가 스키마를 만들지 않고
 *       Flyway가 소유한 실제 마이그레이션 스키마(V1~V17, V16 갭 포함)를 검증만 한다.</li>
 *   <li>{@code app.encryption.enabled=true} + 고정 키로 prod 동등 암호화를 켠다. 평문이 통과하던
 *       H2 기반 테스트의 거짓 통과를 막는다.</li>
 *   <li>JWT 시크릿은 {@code application-test.yml}의 고정값을 그대로 사용한다(여기서 덮지 않음).</li>
 * </ul>
 *
 * <p>Redis는 {@code @ActiveProfiles("test")}가 비활성화하고, 필요 시 상속 클래스가
 * {@link com.cotalk.config.TestRedisConfiguration}를 {@code @Import}해 모킹한다.</p>
 *
 * @author seunggu.lee
 */
@ActiveProfiles("test")
public abstract class PostgresIntegrationTestBase {

    /**
     * prod 동등 스키마 검증을 위한 PostgreSQL testcontainer (수동 싱글톤).
     *
     * <p>static 초기화로 한 번만 기동되며 명시적으로 stop하지 않는다(Ryuk가 JVM 종료 시 정리).
     * 모든 하위 통합 테스트 클래스가 이 컨테이너를 공유한다.</p>
     */
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("cotalk")
                .withUsername("test")
                .withPassword("test");
        // Docker가 없으면(예: CI 외 환경) 시작에 실패한다. @Testcontainers(disabledWithoutDocker=true)와
        // 달리 수동 싱글톤은 자동 비활성화가 없으므로, 시작 실패 시 명확한 메시지로 알린다.
        POSTGRES.start();
    }

    /**
     * PostgreSQL + 실제 Flyway + 암호화 ON으로 prod 동등 환경을 구성한다.
     *
     * <p>{@code application-test.yml}의 H2/암호화 OFF 설정을 동적으로 덮어쓴다.</p>
     *
     * @param registry 동적 프로퍼티 레지스트리
     */
    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL + 실제 Flyway 마이그레이션(V1~V17)으로 prod 동등 스키마 구성
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Hibernate는 스키마를 만들지 않고 Flyway 소유 스키마를 검증만 한다.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // 핵심: prod 동등 암호화 ON + 고정 키 (평문 통과로 인한 거짓 통과 차단)
        registry.add("app.encryption.enabled", () -> "true");
        registry.add("app.encryption.key", () -> "dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=");
        registry.add("app.search.blind-index-secret",
                () -> "dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3Q=");
    }
}
