# 사이드 프로젝트를 프로덕션에 올리기까지 — Co-Talk 보안·백업·마이그레이션 정비기

> 코드는 돌아가는데, 이걸 진짜 서버에 올려도 되는 건가?

---

기능 구현에 집중하다 보면 어느 순간 이런 생각이 든다. 회원가입도 되고, 채팅도 되고, 파일 업로드도 된다. 테스트도 통과한다. 근데 막상 도메인 연결하고 퍼블릭 서버에 올리려니 뭔가 불안하다.

그 불안의 정체를 직접 확인하고 싶었다. Co-Talk 프로젝트에서 프로덕션 준비 리뷰를 진행했고, 결과는 예상보다 훨씬 많은 이슈가 쏟아졌다. 이 글은 그 과정에서 발견한 것들과, 각각을 어떻게 정비했는지 기록이다.

---

## 무엇을 발견했나

프로덕션 준비 리뷰의 첫 번째 작업은 `application.yml`을 처음부터 끝까지 읽는 것이었다. 개발하면서 조금씩 추가하다 보면 그냥 지나치게 되는 것들이 있다.

핵심 발견 사항은 다섯 가지였다.

1. 민감 설정이 하드코딩되어 있거나 기본값으로 노출되어 있다
2. Actuator 엔드포인트가 너무 많이 열려 있다
3. 캐시가 인메모리라 멀티 인스턴스에서 불일치가 생긴다
4. 백업 전략이 없다
5. 스키마 변경을 코드로 관리하지 않고 있다

하나씩 들여다보자.

---

## 1. 시크릿 관리 — Fail-fast가 정답이다

### 문제: 하드코딩된 기본값

초기 `application.yml`에는 이런 설정이 있었다.

```yaml
minio:
  access-key: ${MINIO_ACCESS_KEY:minioadmin}  # 환경변수 없으면 minioadmin
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
app:
  encryption:
    key: ${ENCRYPTION_KEY:dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=}
```

Spring의 `${변수:기본값}` 문법은 편리하지만, 프로덕션 자격증명에 적용하면 위험하다. 환경변수 설정을 빠트려도 에러 없이 기본값으로 실행된다. "잘 돌아가네"라고 생각하면서 알고 보면 `minioadmin`으로 MinIO에 접근하고 있는 것이다.

더 심각한 문제도 있었다. `.env` 파일에 실제 프로덕션 자격증명이 그대로 커밋되어 있었다.

```bash
# .env (커밋된 상태 — 이미 노출됨)
DB_PASSWORD=zhxhrrkdfurgksqlalfqjsgh1!
JWT_SECRET=zhxhrdptjtkdydgksmsjwtqlalfqjsghrkdfurgkrpaksemfwk1!
MINIO_ACCESS_KEY=alsdkdldhrkdfurgksqlalfqjsgh1!
```

`.gitignore`에 `.env`가 등록되어 있었지만, 등록 전에 이미 한 번 커밋했던 거다. Git 히스토리에는 영원히 남는다.

### 해결: 기본값 제거 + 즉시 실패

```yaml
# After — 기본값 없음
minio:
  access-key: ${MINIO_ACCESS_KEY}   # 필수 환경변수 - 기본값 없음
  secret-key: ${MINIO_SECRET_KEY}   # 필수 환경변수 - 기본값 없음
app:
  encryption:
    key: ${ENCRYPTION_KEY}          # 필수 환경변수 - 기본값 없음
jwt:
  secret: ${JWT_SECRET}             # 필수 환경변수 - 최소 256비트(32자) 이상 필요
```

기본값이 없으면 환경변수가 설정되지 않았을 때 애플리케이션이 시작 단계에서 바로 실패한다. Fail-fast 원칙이다. "실행은 되는데 이상한 동작"보다 "시작 자체를 거부"하는 편이 훨씬 낫다.

노출된 자격증명은 모두 즉시 교체했다. PostgreSQL 비밀번호, JWT 시크릿, MinIO 키 전부. 이미 노출된 자격증명을 그냥 두는 건 의미가 없다.

새로운 자격증명 생성은 `openssl`로 충분하다.

```bash
# JWT 시크릿 (32바이트, Base64)
openssl rand -base64 32

# AES-256 암호화 키 (32바이트, Base64)
openssl rand -base64 32

# DB 비밀번호 (24바이트)
openssl rand -base64 24
```

> 이 변경이 나중에 예상치 못한 부작용을 만든다. 기본값을 제거한 `ENCRYPTION_KEY`가 테스트 프로파일에도 설정되어 있지 않으면 CI에서 `IllegalArgumentException`으로 테스트 전체가 터진다. 이 문제와 씨름한 이야기는 11편에서 다룬다.

---

## 2. Actuator 보안 강화 — 최소 노출 원칙

Spring Boot Actuator는 애플리케이션 상태를 모니터링할 수 있는 강력한 도구다. 그런데 기본 설정을 그대로 두면 너무 많은 정보가 노출된다.

### 문제: 민감 엔드포인트 오픈

<!-- IMAGE: Actuator 엔드포인트 목록 스크린샷 (보안 설정 전) — 로컬에서 http://localhost:8080/actuator 접속 시 env, loggers 포함한 전체 엔드포인트 목록 JSON 응답 캡처 -->

초기 설정에서 `env`와 `loggers` 엔드포인트가 열려 있었다.

```yaml
# Before
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers,caches
```

`/actuator/env`를 호출하면 애플리케이션의 모든 환경변수가 응답으로 돌아온다. `spring.datasource.password`는 `****`로 마스킹되지만, 모든 설정이 다 나오는 건 아니다. `MINIO_ENDPOINT`, `REDIS_HOST`, 내부 서비스 URL 등 공격자에게 유용한 인프라 정보가 그대로 노출된다.

`/actuator/loggers`는 런타임에 로그 레벨을 변경할 수 있다. POST 요청으로 `com.cotalk` 패키지의 로그 레벨을 `TRACE`로 올리면, 이후 모든 요청의 상세 로그가 남는다.

### 해결: 필요한 것만 열기

```yaml
# After
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
        # env, loggers 제거 — 환경변수/로그레벨 노출 방지
  endpoint:
    health:
      show-details: when-authorized  # 인증된 사용자만 상세 정보 확인
```

헬스 체크(`health`), 메트릭 수집(`metrics`, `prometheus`), 캐시 모니터링(`caches`) 정도면 운영에 필요한 대부분을 커버한다. `env`와 `loggers`는 불필요한 위험을 감수할 이유가 없다.

---

## 3. 인메모리 캐시 → Redis 캐시 전환

### 문제: 멀티 인스턴스 캐시 불일치

처음에는 사용자 정보 캐싱을 `ConcurrentMapCacheManager`로 구현했다.

```java
// Before — 단일 인스턴스에서는 문제없음
@Bean
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("users", "chatRooms", "statistics");
}
```

JVM 힙 메모리에 직접 올라가는 방식이라 빠르다. 단일 인스턴스에서는 아무 문제가 없다.

문제는 스케일 아웃할 때다. 인스턴스 A에서 사용자 프로필을 수정하면, A의 캐시는 무효화된다. 그런데 인스턴스 B는 여전히 이전 데이터를 캐시에 가지고 있다. 로드밸런서가 다음 요청을 B로 보내면 사용자는 수정하기 전 프로필을 보게 된다.

"지금 당장 스케일 아웃하지 않더라도, 나중에 고치려면 훨씬 더 많은 코드를 건드려야 한다." 이 생각으로 미리 전환했다.

### 해결: Redis 캐시 매니저

```java
// After — 모든 인스턴스가 동일한 Redis 캐시를 바라봄
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
    cacheConfigurations.put("users",     defaultConfig.entryTtl(Duration.ofHours(1)));
    cacheConfigurations.put("chatRooms", defaultConfig.entryTtl(Duration.ofMinutes(30)));
    cacheConfigurations.put("statistics", defaultConfig.entryTtl(Duration.ofMinutes(5)));

    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
}
```

캐시별 TTL을 다르게 설정했다.

| 캐시 | TTL | 이유 |
|------|-----|------|
| `users` | 1시간 | 프로필 변경 빈도가 낮음 |
| `chatRooms` | 30분 | 채팅방 정보 변경 적당한 빈도 |
| `statistics` | 5분 | 통계는 실시간성이 중요 |

`@Profile("!test")`를 붙인 이유가 있다. 테스트 환경에서는 Redis가 없고 인메모리 캐시를 써야 하기 때문이다. 테스트 전용 CacheManager를 따로 선언하는 방식이 깔끔하다.

> 사실 여기서 숨겨진 함정이 있다. `GenericJackson2JsonRedisSerializer`는 `LocalDateTime` 같은 Java 8 날짜 타입을 직렬화할 때 기본 Jackson 설정으로는 에러가 난다. `JavaTimeModule`을 명시적으로 등록해야 한다. 이 직렬화 문제는 5편에서 자세히 다룬다.

---

## 4. 백업 전략 — 없어서 안 된다

"백업이 없는 데이터는 없는 거나 마찬가지다."

프로덕션 서버에 PostgreSQL, Redis, MinIO 세 가지 데이터 저장소가 있다. 각각 다른 백업 전략이 필요하다.

### PostgreSQL — pg_dump + 압축 + 자동 교체

`docker/backup/backup.sh`:

```bash
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.sql.gz"

# pg_dump으로 전체 덤프 후 gzip 압축
pg_dump -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
    --format=plain \
    --no-owner \
    --no-privileges \
    | gzip > ${BACKUP_FILE}

# SHA256 체크섬 생성 (복원 전 무결성 검증용)
sha256sum "$BACKUP_FILE" > "${BACKUP_FILE}.sha256"

# 오래된 백업 자동 삭제 (기본 7일 보관)
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${BACKUP_RETENTION_DAYS} -delete
```

`--no-owner`와 `--no-privileges`는 복원 시 특정 PostgreSQL 유저에 종속되지 않게 하기 위해서다. 다른 환경에서 복원할 때 권한 문제가 생기는 걸 방지한다.

SHA256 체크섬을 함께 저장하는 건 "백업 파일이 깨졌는데 몰랐다"는 최악의 상황을 막기 위해서다. 복원하기 전에 체크섬을 대조해서 무결성을 확인할 수 있다.

### 복원 스크립트 — FORCE_RESTORE 옵션

`docker/backup/restore.sh`:

```bash
# 기존 스키마 정리 후 복원
psql -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO ${POSTGRES_USER};"

gunzip -c ${BACKUP_FILE} | psql -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB}
```

기본적으로 "정말 복원하시겠습니까?" 확인 프롬프트가 있다. 실수로 실행했을 때 데이터를 날리는 사고를 막기 위해서다. CI나 자동화 환경에서는 `FORCE_RESTORE=true`로 프롬프트를 건너뛸 수 있다.

```bash
# 수동 복원 (프롬프트 있음)
docker compose -f docker-compose.backup.yml run --rm restore cotalk_20260205_030000.sql.gz

# 자동화 환경 복원 (프롬프트 없음)
FORCE_RESTORE=true docker compose -f docker-compose.backup.yml run --rm restore cotalk_20260205_030000.sql.gz
```

### Redis / MinIO 전략

Redis는 AOF(Append-Only File)를 켜두는 것으로 기본 보호는 충분하다. Co-Talk에서 Redis는 Pub/Sub 브로커와 세션 캐시로 쓰는데, Pub/Sub 메시지는 순간 소비되고 캐시는 재구성 가능하므로 PostgreSQL만큼 엄격한 백업이 필요하지 않다.

MinIO는 로컬 파일 시스템에 저장하기 때문에 `mc mirror`로 볼륨 전체를 복사하는 방식이 가장 간단하다.

### 자동 백업 cron

```bash
# 매일 새벽 3시 자동 실행
docker compose -f docker-compose.backup.yml up -d backup-cron
```

트래픽이 가장 적은 새벽 3시를 선택했다. `pg_dump`는 배타 락 없이 동작하지만 시스템 부하는 올라가기 때문이다.

---

## 5. Flyway 도입 — DDL을 코드로 관리하다

### 문제: 스키마 변경을 어떻게 추적하나

초반에는 `ddl-auto: create-drop`으로 JPA가 스키마를 자동 생성했다. 개발 초기에는 편리하다. 엔티티 클래스 바꾸면 DB 스키마가 알아서 바뀐다.

그런데 프로덕션에서는 절대 쓸 수 없는 설정이다. 배포할 때마다 데이터가 전부 날아간다.

그렇다고 수동으로 운영 DB에 접속해서 `ALTER TABLE`을 실행하는 것도 문제다. 어떤 변경이 언제 적용됐는지 추적이 안 된다. 롤백도 어렵다. 팀이 커지면 더 엉망이 된다.

Flyway는 이 문제를 SQL 파일 버전 관리로 해결한다.

<!-- IMAGE: Flyway 마이그레이션 히스토리 스크린샷 — psql 또는 DBeaver에서 `SELECT * FROM flyway_schema_history ORDER BY installed_rank;` 실행 결과 캡처. 버전별 실행 일시와 체크섬이 보여야 함 -->

### 마이그레이션 파일 구조

```
src/main/resources/db/migration/
├── V2__add_indexes.sql
├── V3__add_user_role.sql
├── V4__add_last_read_message_id.sql
└── V5__add_link_preview_to_messages.sql
```

파일명이 곧 버전이다. `V2__`, `V3__` 순서대로 한 번씩만 실행된다. 이미 실행된 파일은 체크섬으로 관리되어, 내용이 바뀌면 Flyway가 에러를 낸다.

### 설정

`build.gradle.kts`에 의존성 추가:

```kotlin
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

`application.yml` 설정:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # create-drop 대신 validate
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: '0'
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
```

`ddl-auto: validate`는 JPA가 스키마를 건드리지 않고, 현재 DB 스키마와 엔티티 클래스가 일치하는지만 검증한다. 실제 스키마 변경은 Flyway SQL 파일이 담당한다.

### 이미 운영 중인 DB에 처음 적용할 때

`baseline-on-migrate: true`와 `baseline-version: '0'`이 핵심이다. 기존 스키마를 V0으로 간주하고 `flyway_schema_history` 테이블을 생성한 뒤, 이후 V2부터 순서대로 실행한다. 이미 반영된 스키마를 다시 실행하지 않는다.

### 테스트 환경에서의 함정

Flyway를 켠 뒤 곧바로 테스트가 깨졌다. H2 인메모리 DB를 쓰는 테스트에서 V2 마이그레이션의 이 구문이 문제였다.

```sql
-- PostgreSQL 전용 문법 — H2에서는 지원 안 됨
CREATE INDEX IF NOT EXISTS idx_messages_content_search
    ON messages USING gin(to_tsvector('simple', content));
```

GIN 인덱스는 PostgreSQL 전용이다. H2는 모른다. 테스트를 돌리면 `SQLException`이 터진다.

해결 방법은 테스트 프로파일에서 Flyway를 끄는 것이다. JPA의 `ddl-auto: create-drop`으로 H2에 테스트용 스키마를 만들게 한다.

```yaml
# application-test.yml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
```

이 설정을 빠트린 테스트 프로파일 때문에 CI가 계속 터지는 문제가 이어졌다. 그 이야기는 11편에서 자세히 다룬다.

---

## 프로덕션 준비 체크리스트 돌아보기

이번 PR에서 처리한 항목들을 정리하면 이렇다.

| 항목 | 상태 | 비고 |
|------|------|------|
| 시크릿 노출 해결 | 완료 | `.env` 파일 삭제, 자격증명 교체 |
| 기본 자격증명 제거 | 완료 | Fail-fast 원칙 적용 |
| Actuator 보안 강화 | 완료 | `env`, `loggers` 엔드포인트 제거 |
| Redis 분산 캐시 전환 | 완료 | 멀티 인스턴스 캐시 일관성 확보 |
| 백업 전략 구축 | 완료 | PostgreSQL + Redis + MinIO 자동 백업 |
| Flyway 마이그레이션 도입 | 완료 | DDL 버전 관리 시작 |
| 프로덕션 준비 문서화 | 완료 | `docs/PRODUCTION_READINESS.md` |

---

## 마무리

"일단 돌아가게 만든다"와 "프로덕션에 올릴 수 있게 만든다"는 다른 일이다.

돌아가는 코드를 만드는 건 기능 구현이다. 프로덕션에 올릴 수 있게 만드는 건 그 코드가 낯선 환경에서도 예측 가능하게 동작하고, 문제가 생겼을 때 빠르게 복구할 수 있고, 공격 표면을 최소화하는 작업이다.

사이드 프로젝트라고 해서 이 과정을 건너뛸 이유는 없다. 오히려 사이드 프로젝트이기 때문에 실수해도 되는 환경에서 이런 것들을 배울 수 있다.

물론 이번 정비가 끝이 아니다. Redis 캐시로 전환하면서 `LocalDateTime` 직렬화 문제가 생겼고 (5편), Flyway를 켠 뒤 테스트 환경에서 H2 비호환 문제가 터졌다 (11편). 하나를 해결하면 다음 문제가 기다리고 있다. 그게 프로덕션 준비 과정이다.

---

*Co-Talk 시리즈 다른 글:*
- *1편 — 실시간 채팅 백엔드를 처음부터 만들어보며 배운 것들*
- *2편 — 로컬 올 그린, CI 올 레드 (Spring Boot 통합 테스트 트러블슈팅)*
- *5편 — Redis LocalDateTime 직렬화 함정 (예정)*
- *11편 — Flyway + H2 비호환, 테스트 프로파일 관리법 (예정)*
