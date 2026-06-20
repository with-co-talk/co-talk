# 릴리스 런북 — 메시지 검색 복구 + 분산락 (BLIND_INDEX_SECRET 도입)

> 이번 릴리스 대상: 메시지 검색 복구(블라인드 인덱스, #173/#174), 분산락 fail-closed(#175).
> 핵심 리스크: **신규 필수 환경변수 `BLIND_INDEX_SECRET` 미등록 시 앱이 기동에 실패**한다(fail-fast).
> 이 런북은 해당 배포의 사전 준비 → 배포 → 백필 → 스모크 검증 → 롤백 → 모니터링 절차를 다룬다.

---

## 0. 이번 릴리스 변경 요약 (체크리스트)

| 항목 | 내용 | 운영 조치 |
|------|------|-----------|
| 신규 필수 env | `BLIND_INDEX_SECRET` (Base64 32B, HMAC 검색 토큰 시크릿) | **GitHub Secrets + 운영 `.env`에 등록 필수** |
| 신규 옵션 env | `SEARCH_BACKFILL_ENABLED`(기본 false), `SEARCH_BACKFILL_CHUNK_SIZE`(500), `SEARCH_BACKFILL_THROTTLE_MILLIS`(0), `SEARCH_BACKFILL_SKIP_EXISTING`(true) | 백필 윈도우에만 조정 |
| 락 정책 env | `LOCK_FAIL_CLOSED` (prod 기본 true, application-prod.yml) | 기본값 존재 — 미설정해도 기동됨(확인만) |
| 신규 마이그레이션 | `V17__add_message_search_token.sql` (`message_search_tokens` 테이블 + 인덱스 생성) | Flyway가 기동 시 **자동 적용** (아래 4절) |

> **fail-fast 근거**: `HmacBlindIndexTokenizer` 생성자가 `app.search.blind-index-secret`(=`BLIND_INDEX_SECRET`)이
> 비어 있으면 `IllegalStateException`을 던진다 → 빈 생성 실패 → 애플리케이션 컨텍스트 기동 실패.
> 즉 secret이 없으면 **새 앱 100% 기동 실패**한다.

---

## 0-1. 인프라 토폴로지 — MinIO는 NAS 외부 운영 (상시 사실)

> 이 절은 특정 릴리스가 아니라 **현재 인프라 구성의 상시 사실**이다. minio 관련 배포 사고를 막기 위해 항상 참고한다.

- **MinIO는 운영 머신(맥미니)에 띄우지 않는다.** NAS에서 외부 운영하며, 내부 접근 주소는 **`http://192.168.219.104:9000`** 이다(맥미니 호스트에서 HTTP 200 도달 확인).
- **app**은 `MINIO_ENDPOINT` 환경변수로 NAS 주소를 가리킨다(업로드·presigned URL 생성용).
  - `docker-compose.yml`/`docker-compose.nas.yml`의 app 서비스: `MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://192.168.219.104:9000}` (기본값 NAS, `.env`로 override 가능).
  - 운영 `.env`는 `deploy.yml`의 "Create .env from secrets"가 재생성하며, `MINIO_ENDPOINT=${{ vars.MINIO_ENDPOINT || 'http://192.168.219.104:9000' }}` 라인으로 주입된다(secret 아닌 설정값이라 Actions **variable** 또는 하드코딩 기본값).
- **nginx**(`docker/nginx/nginx.conf`)의 `location /minio/`는 `proxy_pass http://192.168.219.104:9000/;` 로 **IP를 직접** 가리킨다.
  - **IP 직접 사용이 핵심**: docker 서비스명(`minio:9000`)이면 nginx 시작 시 DNS resolve가 필요한데, 로컬 minio 컨테이너가 없으면 resolve 실패로 nginx가 **crash-loop**에 빠져 8081(API)이 죽고 **이후 모든 배포가 실패**한다(#173 이후 사고의 직접 원인). IP는 resolve가 불필요하므로 안전하다.
- **`docker-compose.yml`(맥미니)에서 로컬 minio 서비스는 제거**됐다. `scripts/deploy.sh`도 `dc up -d postgres redis`로만 인프라를 띄운다(minio 제외).
- **`docker-compose.nas.yml`의 minio 서비스는 유지**된다 — 이 파일은 NAS(Synology) 자체 배포용이며 minio가 Synology 볼륨 `/volume1/docker/co-talk/minio`를 사용한다(NAS가 직접 minio를 운영하는 주체). NAS에서 app이 같은 docker 네트워크의 `minio`를 쓰려면 그 머신의 `.env`에서 `MINIO_ENDPOINT=http://minio:9000`으로 override 한다.
- **`MINIO_PUBLIC_URL`**(클라이언트가 보는 외부 다운로드 URL)은 변경 없음.

### NAS IP 변경 시 수정 위치
1. `docker/nginx/nginx.conf` 의 `location /minio/` `proxy_pass` 줄.
2. app `MINIO_ENDPOINT` 기본값(`docker-compose.yml`/`docker-compose.nas.yml`) 또는 운영 `.env`/Actions `vars.MINIO_ENDPOINT`.
3. `.env.example` 의 `MINIO_ENDPOINT`.

### ⚠️ 경고 — dev compose로 운영 머신에 minio 띄우지 말 것
- `docker-compose.dev.yml`(로컬 개발용 minio)을 **운영 머신(맥미니)에서 실행하지 않는다.** 운영 minio는 NAS 단일 소스이며, 운영 머신에 로컬 minio 컨테이너를 띄우면 데이터가 둘로 갈라지고, 과거처럼 nginx가 docker 서비스명을 resolve하려다 crash-loop를 재현할 수 있다(이번 사고의 원인).

---

## 1. 사전 준비 (배포 전 1회)

### 1-1. `BLIND_INDEX_SECRET` 생성
- **`ENCRYPTION_KEY`와 반드시 다른** 별도 값으로 새로 생성한다(키 분리가 블라인드 인덱스 보안의 단일 의존점).
- Base64 인코딩된 32바이트 시크릿:

  ```bash
  openssl rand -base64 32
  ```

> ⚠️ 생성한 실제 값은 이 런북·커밋·로그에 절대 기록하지 않는다. 생성·등록 방법만 문서화한다.

### 1-2. 등록 위치 (두 곳 모두)
1. **GitHub Secrets** (자동 배포 파이프라인용)
   - 저장소 → Settings → Secrets and variables → Actions → New repository secret
   - Name: `BLIND_INDEX_SECRET`, Value: 위에서 생성한 값
   - `.github/workflows/deploy.yml`이 deploy/rollback 두 잡 모두에서 이 secret을 `.env`에 주입한다.
2. **운영 호스트 `.env`** (수동 기동/디버깅용)
   - 운영 스택의 `.env`에 `BLIND_INDEX_SECRET=<생성한 값>` 추가
   - `.env.example`의 `BLIND_INDEX_SECRET` 항목 참고

### 1-3. 사전 확인
- [ ] `BLIND_INDEX_SECRET`이 `ENCRYPTION_KEY`와 다른 값인지 확인
- [ ] GitHub Secrets에 `BLIND_INDEX_SECRET` 등록 완료
- [ ] 운영 `.env`에 `BLIND_INDEX_SECRET` 등록 완료
- [ ] (선택) `SEARCH_BACKFILL_ENABLED`은 배포 시점에 **false**인지 확인(기본 false)

---

## 2. 배포

1. `main` 브랜치에 머지 → push 시 `.github/workflows/deploy.yml`이 자동 트리거.
   - 파이프라인: `.env` 생성(secrets 주입, `BLIND_INDEX_SECRET` 포함) → Canary 배포 → 2분 관찰 →
     canary health(`/actuator/health/liveness`) UP 확인 → stable 승격.
2. 배포 직후 컨테이너 기동 로그 확인:
   - `BLIND_INDEX_SECRET` 미주입이면 `HmacBlindIndexTokenizer` 생성 실패 로그와 함께 기동 실패한다.
     이 경우 1-2 등록을 재확인하고 재배포한다.

---

## 3. 백필 (배포·검증 후, 별도 윈도우)

> 기존(릴리스 이전) 메시지에는 검색 토큰이 없어 **과거 메시지는 검색되지 않는다**.
> 검색 토큰을 채우는 1회성 작업이 백필이다. 신규 메시지는 백필 없이도 검색된다.

1. 트래픽이 낮은 윈도우를 선택한다.
2. `SEARCH_BACKFILL_ENABLED=true`로 앱을 **1회 기동**한다(운영 `.env` 또는 환경변수 override).
   - 동작 근거: `SEARCH_BACKFILL_ENABLED`(→ `app.search.backfill.enabled`)가 `true`일 때만
     `MessageSearchBackfillRunner`(`@ConditionalOnProperty`, `ApplicationRunner`)가 빈으로 등록되어
     **기동 직후 백필을 1회 실행**한다. 스케줄러가 아니라 기동 시 1회성이라, **완료 후 자동으로
     꺼지지 않는다** — `true`인 채로 재기동하면 매번 다시 실행된다(아래 4번으로 반드시 복귀).
   - 옵션 튜닝:
     - `SEARCH_BACKFILL_CHUNK_SIZE`(기본 500): 한 트랜잭션 당 메시지 수. 부하가 크면 낮춘다.
     - `SEARCH_BACKFILL_THROTTLE_MILLIS`(기본 0): 청크 사이 슬립(ms). DB 부하 제어용으로 키운다.
     - `SEARCH_BACKFILL_SKIP_EXISTING`(기본 true): 이미 토큰이 있는 메시지는 건너뜀(재실행 안전).
3. 기동 로그로 진행/완료를 추적한다(운영자가 완료를 아는 방법). 로그 키워드:
   - 시작: `=== 기존 메시지 검색 토큰 백필 실행기 시작 (app.search.backfill.enabled=true) ===`
   - 진행(청크마다): `백필 진행: 누적 scanned=..., indexed=..., skipped=..., 마지막 커서 id=...`
     (`indexed`=토큰 적재 건수, `skipped`=이미 토큰 있어 건너뛴 건수, `마지막 커서 id`=진척도)
   - **완료(성공)**: `=== 백필 상태=SUCCESS(완료): scanned=..., indexed=..., skipped=..., 마지막 커서 id=... ===`
     → 이 `상태=SUCCESS(완료)` 로그가 떠야 백필이 끝까지 완료된 것이다.
   - 중단/실패: `=== 백필 상태=PARTIAL(중단/실패): ... 재실행 필요 ===`(일부만 반영) 또는
     `=== 백필 상태=FAILED(실패): ... ===`(진행 통계 없이 실패). 두 경우 모두 **완료가 아니며**,
     idempotent하므로 재실행으로 재개한다. (기동 자체는 실패하지 않고 흡수된다.)
4. **`SEARCH_BACKFILL_ENABLED=false`로 되돌리고** 다시 기동한다(다음 재시작 시 재실행 방지).

> `SKIP_EXISTING=true`라 중단·재실행해도 멱등에 가깝다. 그래도 완료 후 반드시 false로 되돌릴 것.
> (`enabled=false`면 러너 빈 자체가 등록되지 않아 백필이 돌지 않는다.)

---

## 4. Flyway / V17 마이그레이션 적용 확인

- **자동 적용된다.** 설정 근거:
  - `application.yml`: `spring.flyway.enabled: true`, `locations: classpath:db/migration`
  - `application-prod.yml`: `flyway.baseline-on-migrate: false`만 override (enabled는 그대로 true)
  - 따라서 prod 기동 시 `V17__add_message_search_token.sql`이 자동 적용되어
    `message_search_tokens` 테이블과 `idx_mst_token_message` 인덱스가 생성된다.
- 확인 방법:
  - 기동 로그에서 Flyway가 `V17`을 migrating/migrated 하는지 확인.
  - DB에서 `SELECT * FROM flyway_schema_history WHERE version='17';` 로 success 여부 확인.
  - `\dt message_search_tokens` (psql)로 테이블 존재 확인.
- **자동 적용 안 될 경우(수동 절차)**:
  - 마이그레이션 위치(`src/main/resources/db/migration`)와 이미지 빌드 포함 여부 확인.
  - 필요 시 `V17__add_message_search_token.sql`을 운영 DB에 직접 실행
    (테이블 `IF NOT EXISTS`, 인덱스 `IF NOT EXISTS`라 재실행 안전).

---

## 5. 스모크 검증

배포·(백필 후) 다음을 순서대로 확인한다.

1. **로그인** — 인증 정상 동작.
2. **메시지 전송** — 새 메시지 송수신 정상.
3. **검색(신규)** — 방금 보낸 메시지가 키워드 검색에 잡히는지 확인.
4. **검색(과거)** — 백필을 수행했다면 릴리스 이전 과거 메시지도 검색되는지 확인.
5. **차단 동작** — 차단 사용자/필터 동작이 정상인지 확인.
6. **분산락 헬스** — 락 컴포넌트 상태 확인.
   - ⚠️ **상세는 인증된 호출에만 보인다**: `application.yml`이 `management.endpoint.health.show-details: when-authorized`라,
     인증 없이 `/actuator/health`를 호출하면 **top-level `{"status":"UP"}`만** 내려가고 컴포넌트별 상세는 가려진다.
     게다가 NoOp+fail-closed=false 강등은 커스텀 `DEGRADED`(가장 낮은 심각도로 집계)라 **top-level은 그대로 `UP`**으로 유지된다.
     즉 인증 없는 top-level UP만으로는 락 강등 여부를 알 수 없다.
   - 상세 확인 방법(택1):
     - **인증된** `/actuator/health` 호출 → 응답의 `components.distributedLock`(컴포넌트 키) 확인.
       - `UP` + `distributedLock: "활성"` → 정상.
       - `DEGRADED` + `failClosed: false` → Redis 미가용으로 NoOp 강등(동시성 보호 없이 요청은 처리됨). 점검 필요.
       - `DOWN` + `failClosed: true` → NoOp + fail-closed(prod 기본). 락 보호 작업이 예외로 거부되는 상태. 즉시 Redis 점검.
     - 인증/상세 노출이 어려우면 **기동·운영 로그를 병행** 확인: NoOp 강등 시 `DistributedLockExecutor`의 NoOp 경고 로그가 남는다(7절 모니터링 참고).

---

## 6. 롤백

- 이전 stable 이미지로 롤백: `./scripts/deploy.sh --rollback`
  (워크플로 수동 트리거 `action: rollback`도 가능).
- **영향 범위 명시**: 블라인드 인덱스는 **검색 기능에만** 영향을 준다.
  메시지 송수신·암호화 저장은 `ENCRYPTION_KEY`로 독립적으로 동작하므로
  검색 토큰/`BLIND_INDEX_SECRET` 문제로 롤백해도 **메시지 데이터 무결성/송수신은 무관**하다.
- V17 마이그레이션은 신규 테이블 추가(additive)라 롤백 시 그대로 둬도 기존 동작에 영향 없다.

---

## 7. 모니터링

- **백필**: 진행/완료/실패 로그. 실패 시 청크 크기·throttle 조정 후 재실행.
- **분산락**: 락 NoOp 경고 로그(`fail-closed=false`에서 Redis 미가용 시 주기적 경고).
  prod는 `LOCK_FAIL_CLOSED` 기본 true라 무방비 실행 대신 예외로 거부됨 — 관련 예외 급증 시 Redis 상태 점검.
- **검색**: 검색 쿼리 0건 급증(토큰 미적재/백필 미완료 신호) 모니터링.

---

## 운영자 TODO 요약

- [ ] `BLIND_INDEX_SECRET` 생성(`openssl rand -base64 32`, `ENCRYPTION_KEY`와 다른 값)
- [ ] GitHub Secrets + 운영 `.env`에 `BLIND_INDEX_SECRET` 등록
- [ ] 배포 후 기동 로그 + Flyway V17 적용 확인
- [ ] 스모크 검증(로그인→전송→검색→차단→락 헬스)
- [ ] 별도 윈도우에서 백필 1회(`SEARCH_BACKFILL_ENABLED=true`→완료 확인→`false` 복귀)
</content>
</invoke>
