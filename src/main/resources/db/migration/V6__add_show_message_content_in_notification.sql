-- 알림 설정 테이블에 메시지 내용 노출 여부 컬럼 추가
ALTER TABLE notification_settings
    ADD COLUMN show_message_content_in_notification BOOLEAN NOT NULL DEFAULT true;
