# DB 마이그레이션 (Flyway)

이 디렉토리는 Flyway 마이그레이션 스크립트(`V{n}__*.sql`)를 보관한다.
운영/스테이징은 `application.yml`의 `spring.flyway.enabled=true`로 기동 시 자동 적용한다.

## 버전 번호 규칙 / 갭

- `spring.flyway.out-of-order=false` (application.yml): 마이그레이션은 **순서대로만** 적용된다.
  중간 버전 번호가 **비어 있어도(갭)** Flyway는 정상 동작한다 — 다음 버전을 이어서 적용할 뿐이다.
  단, 이미 더 높은 버전이 적용된 뒤에 **더 낮은 신규 버전을 끼워넣는 것**(out-of-order)만 거부된다.

## V16 갭은 의도된 것 (오류 아님)

현재 main에는 `V15` 다음에 `V16`이 없고 바로 `V17`로 넘어간다. 이는 **의도된 갭**이다.

- `V16__add_trgm_search_indexes.sql`은 미머지 브랜치(`perf/search-trgm-index`, 커밋 `5f8d5f1`)에서
  메시지 검색을 `pg_trgm` GIN 인덱스로 가속하려던 시도였다.
- 그러나 `messages.content`가 AES-256-GCM(랜덤 IV) **애플리케이션 암호화**로 저장되면서 평문 `LIKE`/trgm
  부분일치가 운영에서 0건이 되는 것이 확인되었고, trgm 인덱스 접근(V16)은 **폐기**되었다.
- 대신 PR #173에서 HMAC-SHA256 트라이그램 토큰 테이블(**블라인드 인덱스**)을 도입한 `V17__add_message_search_token.sql`이
  채택되어 main에 머지되었다. V16은 main에 한 번도 머지되지 않았다.

결론: **V16 번호는 폐기된 마이그레이션의 자리이며 의도적으로 비워 둔다.** `out-of-order=false`로도
Flyway는 V15 → V17을 정상 적용하므로 별도 설정 변경이 필요 없다. **이 번호로 새 마이그레이션을
만들지 말 것**(이미 운영에 V17까지 적용된 환경에 V16을 끼워넣으면 out-of-order로 거부되거나,
켤 경우 정합성이 깨진다).
