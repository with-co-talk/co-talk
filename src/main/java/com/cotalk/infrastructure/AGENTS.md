<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# infrastructure - 인프라스트럭처 계층

## 개요
횡단 관심사와 외부 기술 구현. 보안, 메시징, 암호화, 모니터링 등. 도메인 아웃바운드 포트의 기술적 구현체 포함.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `security/` | Spring Security 설정, JWT, 인증 필터, BCrypt |
| `config/` | Spring 설정 (Redis, Cache, WebMvc, JPA Auditing 등) |
| `config/properties/` | 커스텀 설정 프로퍼티 |
| `messaging/` | Redis Pub/Sub 브로커 + InMemory 대체 구현 |
| `websocket/` | WebSocket STOMP 설정, 인증 인터셉터, 이벤트 리스너 |
| `crypto/` | AES-256 암호화, EncryptedStringConverter |
| `email/` | SMTP/Console 이메일 발송 |
| `push/` | Firebase FCM 푸시 알림 |
| `storage/` | MinIO/InMemory 파일 저장소 |
| `lock/` | Redisson 분산락 |
| `presence/` | Redis/InMemory 채팅방 접속자 추적 |
| `ratelimit/` | Redis 기반 Rate Limiting |
| `id/` | Snowflake ID 생성기 + Redis Worker ID 할당 |
| `metrics/` | Micrometer 커스텀 메트릭 |
| `health/` | Actuator 헬스 인디케이터 (DB, Redis) |
| `persistence/` | JPA 공통 설정 (Converter 등) |
| `exception/` | GlobalExceptionHandler (DomainException → HTTP 응답 변환) |
| `time/` | SystemTimeProvider (TimeProvider 포트 구현) |
| `util/` | 인프라 유틸리티 |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `security/SecurityConfig.java` | Spring Security 설정. JWT Stateless, CSP/HSTS 보안헤더, ADMIN 롤 제한 |
| `security/JwtTokenProvider.java` | JWT 생성/검증 (HMAC-SHA, ACCESS/REFRESH 분리) |
| `security/JwtAuthenticationFilter.java` | 모든 요청에서 JWT 파싱 → SecurityContext 설정 |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`. DomainException.statusHint → HTTP 상태 결정 |
| `messaging/RedisChatMessageBroker.java` | Redis Pub/Sub 채팅 브로드캐스트 |
| `messaging/InMemoryChatMessageBroker.java` | Redis 없을 때 인메모리 대체 |
| `id/SnowflakeIdGenerator.java` | 64비트 Snowflake ID (2024-01-01 에포크) |
| `id/RedisWorkerIdAllocator.java` | 분산 환경 Worker ID 할당 (충돌 방지) |
| `lock/DistributedLockExecutor.java` | Redisson 분산락 (RedissonClient 없을 시 NoOp) |
| `crypto/EncryptionService.java` | AES-256 메시지 암호화 |
| `storage/MinioFileStorage.java` | MinIO 파일 저장소 |
| `push/FcmPushNotificationSender.java` | Firebase FCM 푸시 |
| `websocket/WebSocketConfig.java` | STOMP `/ws` 엔드포인트, `/topic` `/queue` 구독 |
| `ratelimit/RateLimitInterceptor.java` | Redis 기반 요청 제한 |
| `time/SystemTimeProvider.java` | TimeProvider 포트의 실제 구현 |

## AI 에이전트 가이드

### 환경 전환 패턴 (Strategy)
대부분의 인프라 구현체는 Redis/InMemory 이중 구현:
```
@ConditionalOnProperty("redis.enabled", havingValue="true")
→ RedisChatMessageBroker

@ConditionalOnProperty("redis.enabled", havingValue="false", matchIfMissing=true)
→ InMemoryChatMessageBroker
```
이 패턴이 적용된 컴포넌트: ChatMessageBroker, ChatRoomPresenceTracker, UserEventBroker, FileStorage, EmailSender

### 예외 처리 흐름
```
DomainException 발생 → GlobalExceptionHandler 포착
  → DomainException.statusHint.getStatusCode() → HttpStatus 결정
  → ErrorResponse(error, code, timestamp) JSON 반환
```

### 보안 필터 체인
```
HTTP → JwtAuthenticationFilter (JWT → SecurityContext)
  → SecurityFilterChain (URL별 권한)
  → Controller (@AuthenticationPrincipal)

WebSocket → WebSocketAuthInterceptor (STOMP CONNECT 시 JWT)
```

### 테스트
- 각 인프라 컴포넌트별 단위 테스트 (~40개)
- TestRedisConfiguration으로 Redis 관련 빈 모킹
- 실제 Redis 필요한 테스트: `application-ratelimit-test.yml` 프로파일

<!-- MANUAL: -->
