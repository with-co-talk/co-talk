-- 그룹 채팅방 이미지 URL 컬럼 추가
ALTER TABLE chat_rooms ADD COLUMN image_url VARCHAR(2048);
