# Co-Talk 프로덕션 준비 가이드

> 이 문서는 Co-Talk 프로젝트의 프로덕션 배포 준비 과정을 기록합니다.
> 2026년 2월 프로덕션 준비 리뷰에서 발견된 이슈와 해결 방법을 상세히 다룹니다.

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처 요약](#아키텍처-요약)
3. [프로덕션 준비 체크리스트](#프로덕션-준비-체크리스트)
4. [P0 이슈: 즉시 해결 필요](#p0-이슈-즉시-해결-필요)
5. [P1 이슈: 스케일링 준비](#p1-이슈-스케일링-준비)
6. [배포 가이드](#배포-가이드)
7. [운영 가이드](#운영-가이드)
8. [보안 체크리스트](#보안-체크리스트)

---

## 프로젝트 개요

**Co-Talk**은 실시간 메시징 플랫폼으로, 카카오톡/LINE과 유사한 1:1 및 그룹 채팅, 읽음 확인, 푸시 알림, 파일 공유 기능을 제공합니다.

### 기술 스택

| 구성요소 | 기술 | 버전 |
|---------|------|------|
| **백엔드** | Java + Spring Boot | 21 / 3.3.0 |
| **프론트엔드** | Flutter | 3.8+ |
| **데이터베이스** | PostgreSQL | 16 |
| **캐시** | Redis | 7 |
| **파일 저장소** | MinIO (S3 호환) | Latest |
| **모니터링** | Prometheus + Grafana | 2.48 / 10.2 |
| **트레이싱** | Zipkin | Latest |
| **로깅** | Loki | 2.9 |

### 아키텍처 패턴

- **백엔드**: Hexagonal Architecture (Ports and Adapters)
- **프론트엔드**: Clean Architecture + BLoC Pattern
- **통신**: REST API + WebSocket (STOMP)
- **인증**: JWT (Access + Refresh Token)

---

## 아키텍처 요약

```
┌─────────────────────────────────────────────────────────────┐
│                      Flutter Client                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Presentation│  │   Domain    │  │        Data         │  │
│  │   (BLoC)    │→ │ (Entities)  │← │ (Repos, Datasources)│  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP (Dio) / WebSocket (STOMP)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Adapter   │  │ Application │  │       Domain        │  │
│  │ (REST, WS)  │→ │ (Use Cases) │→ │ (Entities, Ports)   │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│         │                                    ↑               │
│         └────────────────────────────────────┘               │
│                    Infrastructure                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │PostgreSQL│  │  Redis   │  │  MinIO   │  │   Firebase   │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 멀티 인스턴스 지원

WebSocket 메시지는 **Redis Pub/Sub**을 통해 모든 인스턴스로 브로드캐스트됩니다:

```java
// RedisChatMessageBroker.java
public void publish(Long roomId, ChatBroadcastMessage message) {
    String channel = channelPrefix + roomId;
    redisTemplate.convertAndSend(channel, jsonMessage);
}

// RedisChatMessageSubscriber.java
public void onMessage(Message message, byte[] pattern) {
    // Redis에서 수신 → WebSocket으로 클라이언트에게 전달
    messagingTemplate.convertAndSend(destination, wsMessage);
}
```

---

## 프로덕션 준비 체크리스트

### 필수 (P0) - 배포 전 반드시 완료

- [x] 시크릿 노출 해결 (.env 파일 정리)
- [x] 기본 크리덴셜 제거 (application.yml)
- [x] 분산 캐시 전환 (Redis)
- [x] Actuator 보안 강화
- [x] 데이터베이스 백업 전략

### 권장 (P1) - 스케일링 전 완료

- [x] 데이터베이스 인덱스 추가
- [x] Flyway 마이그레이션 도입
- [x] Alertmanager 알림 설정

### 선택 (P2) - 운영 중 개선

- [ ] 대용량 코드 파일 리팩토링
- [ ] TODO 주석 이슈화
- [ ] 스킵된 테스트 구현

---

## P0 이슈: 즉시 해결 필요

### 1. 시크릿 노출 문제

#### 문제

`.env` 파일에 실제 프로덕션 크리덴셜이 포함되어 커밋됨:

```bash
# 노출된 항목들
DB_PASSWORD=zhxhrrkdfurgksqlalfqjsgh1!
JWT_SECRET=zhxhrdptjtkdydgksmsjwtqlalfqjsghrkdfurgkrpaksemfwk1!
MINIO_ACCESS_KEY=alsdkdldhrkdfurgksqlalfqjsgh1!
MAIL_PASSWORD=lloxpvnchliesmci
```

Flutter 쪽에서도 Apple Developer 크리덴셜이 노출됨.

#### 해결

1. **`.env` 파일 삭제** (`.gitignore`에는 이미 포함됨)
2. **`.env.example` 유지** (플레이스홀더 값만 포함)
3. **모든 노출된 크리덴셜 즉시 교체**

```bash
# 삭제된 파일
rm co-talk/.env
rm co-talk-flutter/fastlane/.env
```

#### 크리덴셜 교체 체크리스트

- [ ] PostgreSQL 비밀번호
- [ ] JWT 시크릿 키
- [ ] MinIO Access/Secret 키
- [ ] 이메일 앱 비밀번호
- [ ] Apple Developer 크리덴셜 (App Store Connect API 키 재발급)

---

### 2. 기본 크리덴셜 제거

#### 문제

`application.yml`에 개발용 기본값이 포함되어, 환경변수 미설정 시 예측 가능한 값 사용:

```yaml
# Before (위험)
minio:
  access-key: ${MINIO_ACCESS_KEY:minioadmin}  # 기본값 = minioadmin
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
app:
  encryption:
    key: ${ENCRYPTION_KEY:dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=}
```

#### 해결

기본값 제거하여 환경변수 필수화:

```yaml
# After (안전)
minio:
  access-key: ${MINIO_ACCESS_KEY}  # 기본값 없음 - 필수
  secret-key: ${MINIO_SECRET_KEY}  # 기본값 없음 - 필수
app:
  encryption:
    key: ${ENCRYPTION_KEY}  # 기본값 없음 - 필수
```

환경변수 미설정 시 애플리케이션 시작 실패 → **Fail-fast 원칙**

---

### 3. 분산 캐시 전환

#### 문제

`CacheConfig.java`가 인메모리 캐시 사용:

```java
// Before - 멀티 인스턴스에서 캐시 불일치 발생
@Bean
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(USER_CACHE, CHAT_ROOM_CACHE, STATISTICS_CACHE);
}
```

#### 해결

Redis 캐시 매니저로 변경:

```java
// After - 모든 인스턴스가 캐시 공유
@Bean
@Profile("!test")
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put(USER_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
    cacheConfigurations.put(CHAT_ROOM_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
    cacheConfigurations.put(STATISTICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
}
```

#### TTL 설정

| 캐시 | TTL | 용도 |
|------|-----|------|
| `users` | 1시간 | 사용자 프로필 |
| `chatRooms` | 30분 | 채팅방 정보 |
| `statistics` | 5분 | 통계 데이터 |

---

### 4. Actuator 보안 강화

#### 문제

`env`, `loggers` 엔드포인트가 노출되어 민감 정보 접근 가능:

```yaml
# Before
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers,caches
```

#### 해결

민감 엔드포인트 제거:

```yaml
# After
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
        # env, loggers 제거 - 환경변수/로그레벨 노출 방지
```

---

### 5. 데이터베이스 백업 전략

#### 문제

백업 전략 없음 → 데이터 손실 위험

#### 해결

`docker-compose.backup.yml` 및 백업 스크립트 추가:

```bash
# 수동 백업
docker compose -f docker-compose.backup.yml run --rm backup

# 자동 백업 (매일 새벽 3시)
docker compose -f docker-compose.backup.yml up -d backup-cron

# 복원
docker compose -f docker-compose.backup.yml run --rm restore cotalk_20260205_030000.sql.gz
```

#### 사전 준비 (네트워크/볼륨)

`docker-compose.backup.yml`은 메인 `docker-compose.yml`에서 생성한 네트워크와 볼륨을 참조합니다.
백업 서비스 실행 전에 다음 리소스가 존재해야 합니다:

```bash
# 메인 서비스가 실행 중이면 자동으로 생성되어 있음
# 메인 서비스 없이 백업만 실행할 경우 수동 생성 필요:
docker network create cotalk-network
docker volume create co-talk_postgres-data
```

> **참고**: `docker compose up -d`로 메인 서비스를 먼저 시작하면 네트워크와 볼륨이 자동 생성됩니다.

#### 비대화형 복원 (CI/자동화)

자동화 환경에서 복원 시 확인 프롬프트를 건너뛰려면 `FORCE_RESTORE=true`를 설정합니다:

```bash
FORCE_RESTORE=true docker compose -f docker-compose.backup.yml run --rm restore <파일명>
```

#### 백업 스크립트 (`docker/backup/backup.sh`)

```bash
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.sql.gz"

# pg_dump + gzip 압축
pg_dump -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
    --format=plain --no-owner --no-privileges | gzip > ${BACKUP_FILE}

# 오래된 백업 정리 (기본 7일)
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${BACKUP_RETENTION_DAYS} -delete
```

---

## P1 이슈: 스케일링 준비

### 1. 데이터베이스 인덱스

이미 `V2__add_indexes.sql`에 포괄적인 인덱스가 추가됨:

```sql
-- messages 테이블
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_id ON messages(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_id_created_at ON messages(chat_room_id, created_at DESC);

-- chat_room_members 테이블
CREATE INDEX IF NOT EXISTS idx_chat_room_members_user_id ON chat_room_members(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_room_members_chat_room_user ON chat_room_members(chat_room_id, user_id);

-- friends 테이블
CREATE INDEX IF NOT EXISTS idx_friends_user_id ON friends(user_id);
CREATE INDEX IF NOT EXISTS idx_friends_friend_id ON friends(friend_id);

-- Full-text search (PostgreSQL GIN)
CREATE INDEX IF NOT EXISTS idx_messages_content_search ON messages USING gin(to_tsvector('simple', content));
```

### 2. Flyway 마이그레이션

#### 의존성 추가 (`build.gradle.kts`)

```kotlin
// Database Migration
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

#### 설정 (`application.yml`)

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: '0'
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
```

#### 마이그레이션 파일 위치

```
src/main/resources/db/migration/
├── V2__add_indexes.sql
├── V3__add_user_role.sql
├── V4__add_last_read_message_id.sql
└── V5__add_link_preview_to_messages.sql
```

#### 기존 DB에 Flyway 적용 절차

이미 운영 중인 데이터베이스에 Flyway를 처음 도입할 때는 다음 절차를 따릅니다:

1. **현재 스키마 확인**: 운영 DB의 현재 스키마가 마이그레이션 파일과 일치하는지 확인
2. **베이스라인 설정**: `baseline-on-migrate: true`와 `baseline-version: '0'` 설정으로 기존 스키마를 V0으로 간주
3. **검증**: `validate-on-migrate: true`로 마이그레이션 파일의 체크섬 검증 활성화
4. **순서 보장**: `out-of-order: false`로 마이그레이션 순서 보장

```yaml
# application.yml Flyway 설정
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true    # 기존 DB에 flyway_schema_history 테이블 자동 생성
    baseline-version: '0'        # 기존 스키마를 V0으로 간주
    locations: classpath:db/migration
    validate-on-migrate: true    # 체크섬 검증
    out-of-order: false          # 순서 보장
```

> **주의**: 최초 적용 시 `baseline-on-migrate`가 `flyway_schema_history` 테이블을 생성하고 V0을 베이스라인으로 등록합니다. 이후 V2 이상의 마이그레이션만 실행됩니다.

### 3. Alertmanager 알림 설정

환경변수 기반 Slack/이메일 알림 설정:

```yaml
# docker/alertmanager/alertmanager.yml
receivers:
  - name: 'critical-receiver'
    slack_configs:
      - api_url: '${SLACK_WEBHOOK_URL}'
        channel: '#cotalk-critical'
        color: '{{ if eq .Status "firing" }}danger{{ else }}good{{ end }}'
        title: ':rotating_light: [CRITICAL] {{ .GroupLabels.alertname }}'
    email_configs:
      - to: '${ALERT_EMAIL_TO}'
        headers:
          Subject: '[Co-Talk CRITICAL] {{ .GroupLabels.alertname }}'
```

#### 알림 환경변수

```bash
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
ALERT_EMAIL_TO=team@example.com
SMTP_SMARTHOST=smtp.gmail.com:587
SMTP_AUTH_USERNAME=your-email@gmail.com
SMTP_AUTH_PASSWORD=your-app-password
```

---

## 배포 가이드

### 사전 준비

1. **환경변수 설정**:
   ```bash
   cp .env.example .env
   # 모든 CHANGE_ME 값을 실제 값으로 변경
   ```

2. **크리덴셜 생성**:
   ```bash
   # JWT 시크릿 (최소 32자)
   openssl rand -base64 32

   # 암호화 키 (32바이트, Base64)
   openssl rand -base64 32

   # 강력한 비밀번호
   openssl rand -base64 24
   ```

### Docker Compose 배포

```bash
# 1. 환경변수 로드
export $(cat .env | xargs)

# 2. 메인 서비스 시작
docker compose up -d

# 3. 백업 서비스 시작 (자동 백업)
docker compose -f docker-compose.backup.yml up -d backup-cron

# 4. 상태 확인
docker compose ps
curl http://localhost:8080/actuator/health
```

### Kubernetes 배포

```bash
# 1. 시크릿 생성
kubectl create secret generic cotalk-secrets \
  --from-env-file=.env

# 2. 배포
kubectl apply -k k8s/overlays/production

# 3. 상태 확인
kubectl get pods -l app=cotalk
kubectl logs -l app=cotalk --tail=100
```

---

## 운영 가이드

### 모니터링 대시보드

| 서비스 | URL | 용도 |
|--------|-----|------|
| Grafana | `http://localhost:3001` | 메트릭 대시보드 |
| Prometheus | `http://localhost:9090` | 메트릭 쿼리 |
| Zipkin | `http://localhost:9411` | 분산 트레이싱 |
| Alertmanager | `http://localhost:9093` | 알림 관리 |

### 헬스 체크

```bash
# 애플리케이션 상태
curl http://localhost:8080/actuator/health

# 상세 상태 (인증 필요)
curl -u admin:password http://localhost:8080/actuator/health | jq
```

### 로그 확인

```bash
# Docker 로그
docker logs cotalk-app -f --tail=100

# Kubernetes 로그
kubectl logs -l app=cotalk -f --tail=100
```

### 백업 관리

```bash
# 백업 목록 확인
docker compose -f docker-compose.backup.yml run --rm backup ls -la /backups

# 수동 백업
docker compose -f docker-compose.backup.yml run --rm backup

# 복원 (데이터 손실 주의!)
docker compose -f docker-compose.backup.yml run --rm restore <파일명>
```

---

## 보안 체크리스트

### 인증/인가

- [x] JWT 토큰 기반 인증
- [x] Access Token (24시간) + Refresh Token (7일)
- [x] BCrypt 비밀번호 해싱
- [x] OAuth2 지원 (Kakao, Google, Apple)
- [x] Role-based Access Control (@PreAuthorize)

### 통신 보안

- [x] HTTPS 강제 (HSTS 헤더)
- [x] CORS 설정
- [x] CSP (Content Security Policy) 헤더
- [x] X-Frame-Options (Clickjacking 방지)

### 데이터 보안

- [x] 메시지 내용 AES-256-GCM 암호화
- [x] 파일 업로드 Magic Number 검증
- [x] XSS 방지 (HtmlSanitizer)
- [x] SQL Injection 방지 (Parameterized Queries)

### 운영 보안

- [x] Rate Limiting (Bucket4j + Redis)
- [x] Actuator 민감 엔드포인트 제거
- [x] 환경변수 기반 시크릿 관리
- [x] 로그에서 민감 정보 제외

### 추가 권장사항

- [ ] Secrets Manager 도입 (HashiCorp Vault, AWS Secrets Manager)
- [ ] 취약점 스캔 자동화 (Dependabot, Snyk)
- [ ] 침입 탐지 시스템 (IDS)
- [ ] 정기 보안 감사

---

## 버전 히스토리

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-02-05 | 1.0 | 초기 프로덕션 준비 리뷰 및 P0/P1 이슈 해결 |

---

## 참고 자료

- [Spring Boot Production Checklist](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [PostgreSQL Backup Best Practices](https://www.postgresql.org/docs/current/backup.html)
- [Redis Security](https://redis.io/docs/management/security/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
