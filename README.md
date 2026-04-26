# Co-Talk

대화에 집중한 실시간 커뮤니케이션 플랫폼 백엔드

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 25 (Virtual Threads) |
| Framework | Spring Boot 3.5.6 |
| Database | PostgreSQL 16 |
| Cache / Pub-Sub | Redis 7 (Lettuce + Redisson) |
| ORM | Spring Data JPA + QueryDSL 5.1 |
| DB Migration | Flyway |
| Storage | MinIO (S3 호환) |
| Push | Firebase Cloud Messaging |
| Auth | JWT (Access + Refresh Token) |
| Messaging | WebSocket (STOMP) + Redis Pub/Sub |
| Security | Spring Security 6, AES-256 메시지 암호화, Bucket4j Rate Limiting |
| API Docs | SpringDoc OpenAPI 2.8 (Swagger UI) |
| Observability | Prometheus, Grafana, Loki, Zipkin |
| Build | Gradle 9 (Kotlin DSL), JaCoCo (커버리지 60% 강제) |
| CI/CD | GitHub Actions → GHCR → Tailscale VPN → NAS Blue-Green 배포 |
| Container | Docker (multi-stage), Kubernetes |

## 아키텍처

**Hexagonal Architecture (Ports and Adapters)** — ArchUnit으로 의존 방향 자동 검증

```
src/main/java/com/cotalk/
├── adapter/
│   ├── inbound/
│   │   ├── rest/              # REST 컨트롤러 22개 (/api/v1/**)
│   │   └── websocket/         # WebSocket STOMP 컨트롤러
│   └── outbound/
│       └── persistence/       # JPA Repository 어댑터
│           ├── entity/        # JPA 전용 엔티티 (UserJpaEntity 등)
│           └── mapper/        # 도메인 ↔ JPA 변환 매퍼
├── application/
│   └── service/               # UseCase 구현 서비스 (~65개)
├── domain/
│   ├── entity/                # 도메인 엔티티 (User, Message, ChatRoom 등)
│   ├── model/                 # 값 객체 (Email 등 Java record)
│   ├── port/
│   │   ├── inbound/           # UseCase 인터페이스 (~50개)
│   │   └── outbound/          # Repository/서비스 포트 (~30개)
│   ├── exception/             # DomainException + 34개 구체 예외
│   └── validator/             # 도메인 검증기
└── infrastructure/
    ├── security/              # Spring Security, JWT, 인증 필터
    ├── messaging/             # Redis Pub/Sub + InMemory 전략
    ├── websocket/             # STOMP 설정, 인증 인터셉터
    ├── crypto/                # AES-256 메시지 암호화
    ├── lock/                  # Redisson 분산락
    ├── ratelimit/             # Bucket4j 엔드포인트별 Rate Limiting
    ├── id/                    # Snowflake ID 생성기
    └── ...                    # email, push, storage, metrics, health, time
```

### 의존 방향 규칙

- `domain` → 외부 패키지 의존 금지 (순수 Java + Jakarta Validation만)
- `application` → `domain`만 의존
- `adapter/inbound` → `adapter/outbound` 직접 의존 금지

## 주요 기능

### 채팅

- 실시간 1:1 / 그룹 채팅 (WebSocket STOMP)
- 읽음 표시 (카카오톡/라인 스타일 안 읽은 인원 수 표시)
- AES-256 메시지 암호화
- 메시지 검색 (PostgreSQL Full-Text Search)
- 메시지 반응 (이모지 리액션)
- 링크 미리보기 (OG 메타 파싱)
- Redis Pub/Sub 기반 멀티 인스턴스 브로드캐스트

### 사용자

- 이메일 회원가입 / 로그인 / OAuth
- JWT 기반 인증 (Access Token + Refresh Token)
- 비밀번호 재설정 (이메일 인증코드)
- 프로필 관리 및 프로필 변경 이력

### 소셜

- 친구 요청 / 수락 / 거절
- 친구 숨김 / 차단
- 사용자 검색

### 파일 및 알림

- 이미지/동영상 업로드 (MinIO, 최대 15MB)
- FCM 푸시 알림 (알림 설정 커스텀 가능)
- 디바이스 토큰 관리

### 운영

- 엔드포인트별 Rate Limiting (IP/사용자 기반)
- 신고 시스템
- 관리자 API
- 서비스 약관 관리

## REST API 엔드포인트 (22개 컨트롤러)

| 컨트롤러 | 경로 | 설명 |
|----------|------|------|
| AuthController | `/api/v1/auth/**` | 로그인, 회원가입, 토큰 갱신 |
| OAuthController | `/api/v1/oauth/**` | 소셜 로그인 |
| AccountController | `/api/v1/account/**` | 계정 관리, 탈퇴 |
| UserController | `/api/v1/users/**` | 사용자 조회, 검색, 프로필 |
| PasswordController | `/api/v1/password/**` | 비밀번호 변경, 재설정 |
| EmailVerificationController | `/api/v1/email/**` | 이메일 인증 |
| FriendController | `/api/v1/friends/**` | 친구 요청, 수락, 목록 |
| HiddenFriendController | `/api/v1/friends/hidden/**` | 숨긴 친구 관리 |
| BlockController | `/api/v1/blocks/**` | 사용자 차단 |
| ChatRoomController | `/api/v1/chat/rooms/**` | 1:1 채팅방 |
| GroupChatRoomController | `/api/v1/chat/rooms/group/**` | 그룹 채팅방 |
| ChatMessageController | `/api/v1/chat/messages/**` | 메시지 전송, 조회 |
| ChatReactionController | `/api/v1/chat/reactions/**` | 메시지 리액션 |
| MessageSearchController | `/api/v1/chat/search/**` | 메시지 검색 |
| FileController | `/api/v1/files/**` | 파일 업로드/다운로드 |
| LinkPreviewController | `/api/v1/link-preview/**` | URL 미리보기 |
| DeviceController | `/api/v1/devices/**` | FCM 디바이스 토큰 |
| NotificationSettingController | `/api/v1/notifications/**` | 알림 설정 |
| ProfileHistoryController | `/api/v1/profile-history/**` | 프로필 변경 이력 |
| ReportController | `/api/v1/reports/**` | 신고 |
| AdminController | `/api/v1/admin/**` | 관리자 기능 |
| TermsController | `/api/v1/terms/**` | 서비스 약관 |

## 시작하기

### 사전 요구사항

- Java 25 (Temurin 권장)
- Docker & Docker Compose
- Gradle 9+

### 로컬 개발 환경

```bash
# 1. 인프라 실행 (PostgreSQL, Redis, MinIO)
docker-compose -f docker-compose.dev.yml up -d

# 2. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 테스트

```bash
# 전체 테스트 실행 (JaCoCo 커버리지 리포트 자동 생성)
./gradlew test

# 리포트 위치: build/reports/jacoco/test/html/index.html
```

### API 문서

로컬 실행 후 Swagger UI 접속: http://localhost:8080/swagger-ui.html

## 환경 변수

| 변수 | 설명 | 필수 | 기본값 |
|------|------|:----:|--------|
| `DB_USERNAME` | PostgreSQL 사용자명 | O | `cotalk` |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | O | — |
| `JWT_SECRET` | JWT 서명 키 (최소 32자) | O | — |
| `REDIS_HOST` | Redis 호스트 | | `localhost` |
| `REDIS_PORT` | Redis 포트 | | `6379` |
| `REDIS_PASSWORD` | Redis 비밀번호 | | — |
| `MINIO_ENDPOINT` | MinIO 엔드포인트 URL | | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO 접근 키 | O | — |
| `MINIO_SECRET_KEY` | MinIO 비밀 키 | O | — |
| `MINIO_BUCKET` | MinIO 버킷 이름 | | `cotalk` |
| `MINIO_ENABLED` | MinIO 활성화 여부 | | `true` |
| `FIREBASE_ENABLED` | FCM 활성화 여부 | | `false` |
| `FIREBASE_CREDENTIALS_PATH` | FCM 인증 파일 경로 | | `firebase-service-account.json` |
| `ENCRYPTION_KEY` | AES-256 암호화 키 (Base64) | O | — |
| `ENCRYPTION_ENABLED` | 메시지 암호화 활성화 | | `true` |
| `RATE_LIMIT_ENABLED` | Rate Limiting 활성화 | | `true` |
| `MAIL_HOST` | SMTP 호스트 | | — |
| `MAIL_USERNAME` | SMTP 사용자명 | | — |
| `MAIL_PASSWORD` | SMTP 비밀번호 | | — |
| `ZIPKIN_ENDPOINT` | Zipkin 추적 엔드포인트 | | `http://localhost:9411/api/v2/spans` |
| `SERVER_PORT` | 서버 포트 | | `8080` |

## 프로파일

| 프로파일 | 용도 | 설명 |
|----------|------|------|
| `local` | 로컬 개발 | H2 인메모리 DB, Redis 비활성화 |
| `dev` | 개발 서버 | Docker 인프라 연동 |
| `docker` | Docker Compose | 컨테이너 환경 |
| `kubernetes` | K8s 배포 | ConfigMap/Secret 기반 설정 |
| `prod` | 프로덕션 | 보안 강화, JSON 로깅, 샘플링 축소 |

## 인프라 구성

### Docker Compose

| 파일 | 용도 |
|------|------|
| `docker-compose.dev.yml` | 로컬 개발용 인프라 (PostgreSQL, Redis, MinIO) |
| `docker-compose.yml` | 전체 스택 (앱 + 인프라) |
| `docker-compose.nas.yml` | NAS 프로덕션 배포 |
| `docker-compose.monitoring.yml` | 모니터링 스택 (Prometheus, Grafana, Loki, Zipkin) |
| `docker-compose.backup.yml` | DB 백업 |

### CI/CD 파이프라인

```
main push → GitHub Actions → Gradle 테스트 → Docker 이미지 빌드
  → GHCR 푸시 → Tailscale VPN 연결 → NAS SSH → Blue-Green 배포
```

### Kubernetes

```bash
# 개발 환경
kubectl apply -k k8s/overlays/dev

# 프로덕션 환경
kubectl apply -k k8s/overlays/prod
```

HPA, PDB, NetworkPolicy, ServiceMonitor 등 프로덕션 수준의 K8s 리소스 포함.

## 테스트 구조

| 유형 | 어노테이션 | 패턴 |
|------|-----------|------|
| 도메인 단위 | JUnit 5 | 외부 의존 없는 순수 단위 테스트 |
| 서비스 단위 | Mockito | Mock 기반 UseCase 테스트 |
| 컨트롤러 | `@WebMvcTest` | `addFilters=false` |
| 영속성 | `@DataJpaTest` | H2 인메모리 DB |
| 통합 | `@SpringBootTest` | Testcontainers (Redis) |
| 아키텍처 | ArchUnit | 의존 방향·패키지 규칙 자동 검증 |

테스트 메서드명: `should_예상결과_when_조건` 형식

## 핵심 설계 결정

### 인프라 전략 패턴

`@ConditionalOnProperty`로 Redis/InMemory 자동 전환 — Redis 없이도 테스트·개발 가능:
- ChatMessageBroker, ChatRoomPresenceTracker, UserEventBroker, FileStorage, EmailSender

### 엔티티 이중 계층

도메인 엔티티와 JPA 엔티티 분리 — JPA 의존성이 도메인 계층에 침투하지 않음:
- `User` (domain) ↔ `UserJpaEntity` (persistence) + `UserMapper`

### 성능 최적화

- **Virtual Threads**: `spring.threads.virtual.enabled=true` — WebSocket 동시 연결 효율
- **TransactionTemplate**: DB만 트랜잭션, Redis/FCM은 트랜잭션 외부에서 실행
- **Redis 캐시**: `@Cacheable("users")` — User 엔티티 캐싱
- **Snowflake ID**: 64비트 분산 ID 생성 (시간 순서 보장)
- **HikariCP 풀**: 20 최대 / 5 최소 유휴

### 보안

- JWT Access + Refresh Token 이중 인증
- AES-256 메시지 암호화
- 엔드포인트별 Rate Limiting (Bucket4j + Redis)
- Redisson 분산락
- SecurityContext 기반 사용자 식별 (DTO에 userId 미포함)
- 비루트 Docker 컨테이너

## 프로젝트 구조

```
├── .github/workflows/         # CI/CD (GitHub Actions)
├── docker/                    # Docker 관련 설정
├── k8s/                       # Kubernetes 매니페스트
│   ├── base/                  # Deployment, Service, HPA, PDB, NetworkPolicy
│   └── overlays/              # 환경별 (dev, prod)
├── scripts/                   # 배포 스크립트
├── src/
│   ├── main/
│   │   ├── java/              # 소스 코드 (헥사고날 아키텍처)
│   │   └── resources/
│   │       ├── application*.yml   # 프로파일별 설정 (5개)
│   │       └── db/migration/      # Flyway 마이그레이션 (V1~V12)
│   └── test/                  # 테스트 (단위, 통합, 아키텍처)
├── docker-compose*.yml        # Docker Compose (5개)
├── Dockerfile                 # Multi-stage 빌드 (JDK 25 Alpine)
└── build.gradle.kts           # Gradle 9 (Kotlin DSL)
```


Private
