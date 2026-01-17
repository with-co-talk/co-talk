-- 사용자 역할 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- role 컬럼 인덱스 추가 (관리자 조회 최적화)
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
