-- hidden_friends 테이블에 user_id 인덱스 추가
-- 친구 목록 조회 시 숨긴 친구 제외 JPQL 서브쿼리 성능 개선
CREATE INDEX IF NOT EXISTS idx_hidden_friends_user_id ON hidden_friends (user_id);
