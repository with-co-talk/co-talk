-- 사용자 테이블에 전화번호 컬럼 추가
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);

-- 닉네임+전화번호 복합 인덱스 (아이디 찾기용)
CREATE INDEX idx_users_nickname_phone ON users(nickname, phone_number);
