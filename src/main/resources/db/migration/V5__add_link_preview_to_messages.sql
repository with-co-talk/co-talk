-- messages 테이블에 링크 미리보기 컬럼 추가
-- 텍스트 메시지에 포함된 URL의 Open Graph 메타(제목, 설명, 이미지) 저장용
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS link_preview_url VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS link_preview_title VARCHAR(512),
    ADD COLUMN IF NOT EXISTS link_preview_description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS link_preview_image_url VARCHAR(2048);
