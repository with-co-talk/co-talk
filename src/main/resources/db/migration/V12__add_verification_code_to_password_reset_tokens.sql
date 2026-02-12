-- 비밀번호 재설정 토큰 테이블에 인증 코드 컬럼 추가
ALTER TABLE password_reset_tokens ADD COLUMN verification_code VARCHAR(6);

-- 이메일+인증코드 복합 인덱스
CREATE INDEX idx_prt_email_code ON password_reset_tokens(email, verification_code);
