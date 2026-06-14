-- V17: 메시지 검색 블라인드 인덱스 토큰 테이블
-- 배경: messages.content는 AES-256-GCM(랜덤 IV) 애플리케이션 암호화로 저장되어
--       평문 LIKE 검색이 운영에서 0건이 된다(기능 정지). 평문 글자 단위 트라이그램을
--       HMAC-SHA256으로 변환한 결정적 토큰을 별도 테이블에 적재해 등가매칭 조인으로 검색을 복구한다.
-- 전략: token IN (...) AND COUNT(DISTINCT token)=:n 으로 "키워드의 모든 트라이그램을 포함하는 메시지"를
--       조회(1단계) 후, 애플리케이션에서 복호화 substring 검증(2단계)으로 false positive를 제거한다.
-- 인덱스: 토큰은 substring이 아니라 exact match라 trgm/GIN이 불필요하며 일반 B-Tree 하나로 충분하다.

CREATE TABLE IF NOT EXISTS message_search_tokens (
    message_id  BIGINT       NOT NULL,
    token       VARCHAR(24)  NOT NULL,   -- HMAC-SHA256 truncated(12B) → base64url 16자, 여유 24
    PRIMARY KEY (message_id, token),
    CONSTRAINT fk_mst_message FOREIGN KEY (message_id)
        REFERENCES messages(id) ON DELETE CASCADE
);

-- 토큰 → message 역방향 조회용 (검색 쿼리의 워크호스). 신규 테이블이라 CONCURRENTLY 불필요.
-- 1단계 쿼리는 `WHERE token IN (...) GROUP BY message_id HAVING COUNT(DISTINCT token)=:n` 형태라,
-- (token) 단일 인덱스만으로는 매칭 행마다 message_id를 위해 힙(PK)을 다시 들춰야 한다.
-- (token, message_id) 복합 인덱스는 두 컬럼이 모두 인덱스에 있어 인덱스-온리 스캔(커버링)으로
-- 그룹/집계를 끝낼 수 있다(PK가 (message_id, token)이라 이 정렬 순서를 별도로 제공).
CREATE INDEX IF NOT EXISTS idx_mst_token_message ON message_search_tokens(token, message_id);
