-- flyway:executeInTransaction=false
--
-- 메시지 본문·사용자 닉네임 검색 풀스캔 제거: pg_trgm(트라이그램) GIN 인덱스 추가.
--
-- 배경:
--   메시지 검색(MessageJpaRepository.searchByKeywordInChatRoom /
--   searchByKeywordInUserChatRooms)은 한국어 부분 일치(substring) UX를 위해
--   `LOWER(content) LIKE LOWER('%keyword%')` 선행 와일드카드 패턴을 쓴다.
--   사용자 검색(UserJpaRepository.findByNicknameContaining)도 `nickname LIKE '%kw%'`
--   선행 와일드카드다. 선행 와일드카드 LIKE는 B-Tree 인덱스를 탈 수 없어 항상 풀스캔이다.
--
--   #171(V15)은 죽은 to_tsvector GIN 인덱스를 제거했지만, 검색은 여전히 풀스캔이었다.
--   PostgreSQL FTS(to_tsvector)는 공백 단어 경계 기반이라 "비밀번호"에서 "밀번"을 찾는
--   한국어 부분 일치가 불가능하다 → FTS 전환은 검색 UX를 깨므로 부적합.
--
--   해결: pg_trgm 확장 + GIN 인덱스. pg_trgm의 gin_trgm_ops는 LIKE/ILIKE(선행
--   와일드카드 포함)를 인덱스로 가속한다. 검색 쿼리(LIKE 부분 일치)는 그대로 두고
--   인덱스만 추가하므로 한국어 부분 일치 UX가 보존된다.
--
-- 쿼리-인덱스 표현식 일치(핵심):
--   인덱스가 실제로 사용되려면 쿼리의 검색 표현식과 인덱스 표현식이 정확히 일치해야 한다.
--   - 메시지 쿼리는 `LOWER(content) LIKE LOWER(...)` 이므로 `lower(content)` 표현식
--     인덱스를 만든다. (plan: lower(content) ~~ '%...%' 가 trgm GIN을 탄다)
--   - 닉네임 쿼리는 case-sensitive `nickname LIKE '%...%'` 이므로 `nickname` 컬럼에
--     직접 인덱스를 만든다. (대소문자 무시로 바꾸지 않아 동작/결과를 보존)
--
-- 한계:
--   trgm은 3글자(트라이그램) 단위로 동작하므로 1~2글자 키워드는 트라이그램이
--   부족해 인덱스 효과가 제한적이고 여전히 풀스캔/비효율일 수 있다(애플리케이션의
--   keyword 길이 검증은 빈 문자열만 거른다). 3글자 이상 검색에서 효과가 크다.
--
-- 가용성:
--   pg_trgm은 PostgreSQL contrib 표준 확장이다. 대부분의 매니지드 PostgreSQL
--   (RDS/Cloud SQL 등)에서 기본 제공·허용되지만, CREATE EXTENSION 권한이 없는
--   극히 제한된 환경에서는 DBA가 사전에 확장을 활성화해야 한다.
--
-- 운영 잠금:
--   CREATE INDEX CONCURRENTLY로 messages/users 테이블에 ACCESS EXCLUSIVE 락을 잡지
--   않고 인덱스를 빌드한다(쓰기 차단 없음). CONCURRENTLY는 트랜잭션 밖에서만 실행
--   가능하므로 이 마이그레이션은 `executeInTransaction=false`(상단 지시자)로 분리한다.
--
-- 테스트 환경:
--   테스트(H2)는 spring.flyway.enabled=false 이고 ddl-auto: create-drop 으로
--   스키마를 만들므로 이 마이그레이션은 H2에서 실행되지 않는다(pg_trgm 미적용).
--   따라서 H2 비호환으로 인한 테스트 실패는 발생하지 않는다.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 메시지 본문 검색용 trgm GIN 인덱스. 쿼리의 LOWER(content) 와 표현식을 일치시킨다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_content_trgm
    ON messages USING gin (lower(content) gin_trgm_ops);

-- 사용자 닉네임 검색용 trgm GIN 인덱스. 쿼리가 case-sensitive 이므로 nickname 컬럼 직접.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_nickname_trgm
    ON users USING gin (nickname gin_trgm_ops);
