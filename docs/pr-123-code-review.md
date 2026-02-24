# PR #123 코드 리뷰

**PR**: [REFACTOR] 서비스·인프라·설정 개선 및 테스트 보강  
**브랜치**: `refactor/service-infra-and-config-updates`  
**변경**: 27개 파일, +276 / -48

---

## 요약

- **아키텍처**: 헥사고날 원칙 유지, 포트/트랜잭션/시간 주입 적절.
- **보안**: 토큰·이메일 마스킹, Redis 비밀번호·Rate Limit IP 정책 개선.
- **테스트**: 변경된 서비스·컨트롤러·설정에 맞게 테스트 보강.
- **주의**: Actuator Prometheus 공개 제거 시 스크래핑 방식 확인 필요, Redis 비밀번호 운영 설정 필요.

---

## 잘된 점

### 1. 동시성·트랜잭션 일관성

- **AcceptFriendRequestService** / **SendFriendRequestService**: `@Transactional` 제거 후 `TransactionTemplate`으로 락 **내부**에서 트랜잭션 실행. 락-트랜잭션 범위 역전 방지 패턴을 잘 적용함.
- JavaDoc으로 “분산락 + TransactionTemplate” 의도를 명시한 점이 좋음.

### 2. 시간·테스트 용이성

- **DeleteMessageService**, **UpdateMessageService**, **SendMessageService**, **SendPushNotificationService**에서 `System.currentTimeMillis()` / `LocalTime.now()` 대신 **TimeProvider** 사용. 단위 테스트에서 시간 고정이 가능해짐.

### 3. 보안·개인정보

- **RegisterDeviceTokenService**, **FcmPushNotificationSender**: 디바이스/FCM 토큰 로그 출력 시 `maskToken()`으로 마스킹 (앞 6자 + `...` + 뒤 4자).
- **RateLimitInterceptor**: `X-Forwarded-For` 대신 **X-Real-IP** 우선 사용, “클라이언트 조작 가능” 주석으로 의도 명확화.
- **EmailConverter**: 이미 적용된 이메일 마스킹 로깅 유지. PR에서 `convertToDatabaseColumn` JavaDoc 추가로 일관성 향상.

### 4. 예외 처리·API 계약

- **GlobalExceptionHandler**: `HttpMessageNotReadableException`, `ConstraintViolationException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `AccessDeniedException` 처리 추가로 400/403/405/415 응답이 명확해짐.
- **AccountControllerTest**, **TermsControllerTest**: “body 없음 / 잘못된 JSON” 시 **400** 기대하도록 수정해 실제 동작과 일치.

### 5. 로깅 레벨

- **MarkAsReadService**: 채팅 목록 업데이트 관련 상세 로그를 `info` → `debug`로 조정. 운영 로그 노이즈 감소.

### 6. 인프라·설정

- **docker-compose.yml**: Redis 포트 바인딩을 `127.0.0.1:6379:6379`로 제한, Redis에 `--requirepass ${REDIS_PASSWORD}` 적용.
- **backup.sh**: `pg_restore --list`로 백업 무결성 검증, SHA256 체크섬 파일 생성.
- **nginx.conf**: HSTS 헤더 추가, SSL 종료 위치 주석으로 명시.
- **application-prod.yml**: `app.encryption.enabled: true`로 프로덕션에서 암호화 강제.
- **application.yml**: `app.encryption.enabled: ${ENCRYPTION_ENABLED:true}`로 환경별 제어 가능.

### 7. Rate Limit 정책

- **RateLimitWebConfig**: `/api/v1/auth/**` 제외 제거 → 로그인/인증 API에도 Rate Limit 적용. 브루트포스 완화에 유리.
- **RateLimitWebConfigTest**: “인증 API 제외” → “인증 API 포함”으로 기대값 수정.

---

## 개선 제안 (선택)

### 1. Actuator Prometheus 접근 (동작 영향 가능)

**SecurityConfig**에서 `/actuator/prometheus`를 `permitAll()`에서 제거해, 현재는 **ADMIN**만 접근 가능합니다.

- Prometheus가 인증 없이 스크래핑하는 구성이면 **403**으로 메트릭 수집이 실패할 수 있습니다.
- **선택지**:
  - 의도된 것이라면: Prometheus에 Basic Auth 또는 서비스 어카운트를 넣고, Scrape 설정에 반영했는지 확인.
  - 공개 스크래핑이 필요하다면: `permitAll()`에 `/actuator/prometheus`를 다시 넣거나, IP/경로 제한이 있는 별도 보안 규칙 검토.

### 2. Redis 비밀번호 (운영 체크리스트)

- `docker-compose.yml`에서 Redis에 `--requirepass ${REDIS_PASSWORD}` 사용 시, **REDIS_PASSWORD 미설정**이면 빈 비밀번호로 동작할 수 있음.
- 앱은 이미 `application.yml`에서 `password: ${REDIS_PASSWORD:}`를 쓰고 있으므로, **운영/배포 문서**에 “Redis 사용 시 REDIS_PASSWORD 설정 필수”를 명시해 두는 것을 권장.

### 3. 토큰 마스킹 로직 중복 (유지보수성)

- `RegisterDeviceTokenService.maskToken()`과 `FcmPushNotificationSender.maskToken()`이 동일한 규칙(앞 6자 + `...` + 뒤 4자)으로 중복 구현되어 있음.
- 추후 형식 변경 시 두 곳을 같이 수정해야 하므로, 공통 유틸(예: `infrastructure.util.TokenMasker`) 또는 공통 포트/헬퍼로 묶으면 유지보수에 유리함. (현재 상태로 머지해도 무방.)

### 4. 단위 테스트에서 포트 타입 모킹 (일관성)

- **AcceptFriendRequestServiceTest**에서 `DistributedLockExecutor`(구현체)를 모킹하고 있음. 서비스는 `DistributedLockPort`(인터페이스)에만 의존하므로, 테스트에서도 `DistributedLockPort`를 모킹하면 “애플리케이션이 포트에만 의존한다”는 설계와 더 잘 맞습니다. 동작 차이는 없고, 선택적 개선 사항.

---

## 아키텍처·규칙 점검

| 항목 | 상태 |
|------|------|
| domain 패키지에 Spring/JPA 의존 없음 | ✅ |
| application이 adapter/infrastructure 직접 의존 없음 (포트만 사용) | ✅ |
| TransactionTemplate으로 DB만 트랜잭션, Redis/FCM은 외부 | ✅ |
| JavaDoc (public 클래스/메서드) | ✅ |
| 테스트 추가/수정 (AcceptFriend, SendFriend, SendMessage, Push, RateLimit, Account, Terms) | ✅ |

---

## 결론

- 서비스·인프라·설정 개선과 테스트 보강이 일관되게 적용되어 있으며, 헥사고날 원칙과 프로젝트 컨벤션을 잘 따르고 있습니다.
- **머지 권장**. 다만 운영 반영 전에 **Actuator Prometheus 접근 방식**과 **Redis REDIS_PASSWORD 설정**만 한 번 확인하는 것을 추천합니다.

---

## 리뷰 반영 사항 (적용 완료)

- **개선 제안 2 (Redis 비밀번호)**: `docker/AGENTS.md`에 "운영 반영 전 확인" 체크리스트 추가. Redis 사용 시 `REDIS_PASSWORD` 설정 필수 명시.
- **개선 제안 3 (토큰 마스킹 중복)**: `infrastructure.util.TokenMasker` 공통 유틸 추가. `RegisterDeviceTokenService`, `FcmPushNotificationSender`에서 `TokenMasker.mask()` 사용으로 통합.
- **개선 제안 4 (포트 타입 모킹)**: `AcceptFriendRequestServiceTest`에서 `DistributedLockExecutor` 대신 `DistributedLockPort` 모킹으로 변경. 애플리케이션이 포트에만 의존한다는 설계와 일치.
- **개선 제안 1 (Actuator Prometheus)**: `docker/AGENTS.md` 체크리스트에 Prometheus 인증/공개 스크래핑 확인 안내 추가.
