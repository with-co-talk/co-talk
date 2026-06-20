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
   - 옵션 튜닝:
     - `SEARCH_BACKFILL_CHUNK_SIZE`(기본 500): 한 트랜잭션 당 메시지 수. 부하가 크면 낮춘다.
     - `SEARCH_BACKFILL_THROTTLE_MILLIS`(기본 0): 청크 사이 슬립(ms). DB 부하 제어용으로 키운다.
     - `SEARCH_BACKFILL_SKIP_EXISTING`(기본 true): 이미 토큰이 있는 메시지는 건너뜀(재실행 안전).
3. 백필 완료 로그를 확인한다.
4. **`SEARCH_BACKFILL_ENABLED=false`로 되돌리고** 다시 기동한다(다음 재시작 시 재실행 방지).

> `SKIP_EXISTING=true`라 중단·재실행해도 멱등에 가깝다. 그래도 완료 후 반드시 false로 되돌릴 것.

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
6. **분산락 헬스** — `/actuator/health` 조회. 락 헬스 인디케이터가 UP인지,
   Redis 미가용으로 NoOp/fail-closed로 떨어지지 않았는지 확인.

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
