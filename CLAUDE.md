# Co-Talk Project Context

## 프로젝트 개요
실시간 채팅 애플리케이션 백엔드. Java 25 / Spring Boot 3.5.6 / PostgreSQL 16 / Redis 7 / MinIO.

## 패키지 맵 (헥사고날 아키텍처)

```
src/main/java/com/cotalk/
├── adapter/
│   ├── inbound/rest/          # REST 컨트롤러 22개 (/api/v1/**)
│   ├── inbound/websocket/     # WebSocket STOMP 컨트롤러
│   └── outbound/persistence/  # JPA Repository 어댑터 (도메인별 하위 패키지)
│       ├── entity/            # JPA 전용 엔티티 (UserJpaEntity 등)
│       └── mapper/            # 도메인 ↔ JPA 변환 매퍼
├── application/service/       # UseCase 구현 서비스 ~65개 (도메인별 하위 패키지)
├── domain/
│   ├── entity/                # 도메인 엔티티 (User, Message, ChatRoom, Friend 등)
│   ├── model/                 # 값 객체 (Email 등 Java record)
│   ├── port/inbound/          # UseCase 인터페이스 ~50개
│   ├── port/outbound/         # Repository/서비스 포트 ~30개
│   ├── exception/             # DomainException + HttpStatusHint + 34개 구체 예외
│   └── validator/             # 도메인 검증기
└── infrastructure/
    ├── security/              # Spring Security, JWT, 인증 필터
    ├── messaging/             # Redis Pub/Sub + InMemory 대체
    ├── websocket/             # STOMP 설정, 인증 인터셉터
    ├── crypto/                # AES-256 메시지 암호화
    ├── lock/                  # Redisson 분산락
    ├── exception/             # GlobalExceptionHandler
    ├── id/                    # Snowflake ID 생성기
    └── ...                    # email, push, storage, ratelimit, metrics, health, time
```

## 의존 방향 (ArchUnit 자동 검증)
- `domain` → 외부 패키지 의존 금지 (순수 Java + Jakarta Validation만)
- `domain` → `jakarta.persistence`/`org.springframework` 프레임워크 의존 금지 (ArchUnit 강제, `HtmlSanitizer`의 `org.springframework.web.util`만 예외)
- `application` → `domain`만 의존
- `adapter/inbound` → `adapter/outbound` 직접 의존 금지

## 핵심 설계 패턴

### 엔티티 이중 계층 (분리 완료)
- 모든 도메인 엔티티가 순수 POJO + JPA 엔티티 + 매퍼로 완전 분리되었다.
  - 순수 도메인: `domain/entity/<Name>` → `DomainBaseEntity`(JPA 없음, `createdAt`/`updatedAt`) 상속
  - JPA 엔티티: `adapter/outbound/persistence/entity/<Name>JpaEntity` → `BaseJpaEntity`(`@MappedSuperclass` 감사) 상속, 모든 JPA 매핑 보유
  - 매퍼: `adapter/outbound/persistence/mapper/<Name>Mapper` (`@Component`, 도메인 ↔ JPA 변환)
  - 리포지토리 어댑터가 매퍼로 변환하며 도메인 포트를 구현한다.
- 대상 엔티티(16개): `Message`, `ChatRoom`, `ChatRoomMember`, `Friend`, `FriendRequest`, `Block`, `Report`, `RefreshToken`, `PasswordResetToken`, `EmailVerificationToken`, `DeviceToken`, `NotificationSetting`, `MessageReaction`, `HiddenFriend`, `TermsAgreement`, `ProfileHistory` (+ 기존 `User`).
- 메시지 암호화: `content`의 `@Convert(EncryptedStringConverter)`는 `MessageJpaEntity`에 위치하며, 컨버터는 `infrastructure.crypto`로 일원화되었다.
- 페이지네이션: 도메인 포트는 Spring `Page`/`Pageable` 대신 순수 `domain/model/PageQuery`·`PageResult<T>`를 사용하고, persistence 어댑터에서 Spring 타입으로 변환한다.

### 인프라 환경 전환 (Strategy)
`@ConditionalOnProperty`로 Redis/InMemory 자동 전환:
- ChatMessageBroker, ChatRoomPresenceTracker, UserEventBroker, FileStorage, EmailSender

### 예외 처리
`DomainException(errorCode, HttpStatusHint)` → `GlobalExceptionHandler` → HTTP 응답

### 성능
- `TransactionTemplate`: DB만 트랜잭션, Redis/FCM은 트랜잭션 외부
- User 캐시: `@Cacheable("users")` (Redis)
- Snowflake ID: 64비트 분산 ID 생성

## 테스트 구조

| 유형 | 어노테이션 | 위치 | 패턴 |
|------|-----------|------|------|
| 도메인 단위 | JUnit5 | `test/.../domain/` | 외부 의존 없음 |
| 서비스 단위 | Mockito | `test/.../application/service/` | Mock 기반 |
| 컨트롤러 | @WebMvcTest | `test/.../adapter/inbound/rest/` | addFilters=false |
| 영속성 | @DataJpaTest | `test/.../adapter/outbound/persistence/` | H2 인메모리 |
| 통합 | @SpringBootTest | `test/.../integration/` | TestRedisConfiguration |
| 아키텍처 | ArchUnit | `test/.../architecture/` | 의존 방향 검증 |

### 테스트 프로파일 필수 설정 (새 프로파일 추가 시)
```yaml
spring.flyway.enabled: false
jwt.secret: test-secret-key-for-testing-purposes-only-1234567890
firebase.enabled: false
minio.enabled: false
app.encryption.key: dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=
app.encryption.enabled: false
```

## 빌드 & 실행
```bash
./gradlew test                    # 전체 테스트 (JaCoCo 60% 강제)
./gradlew bootRun --args='--spring.profiles.active=dev'
docker-compose -f docker-compose.dev.yml up -d  # 로컬 인프라
```

## 상세 문서
각 디렉토리의 `AGENTS.md` 파일에 상세 가이드 존재.
