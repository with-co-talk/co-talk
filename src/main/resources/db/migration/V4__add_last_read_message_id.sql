-- chat_room_members에 last_read_message_id 추가
-- 카톡/라인 스타일: 멤버별 "어디까지 읽었는지"를 messageId 기준으로 결정적으로 추적한다.

ALTER TABLE chat_room_members
    ADD COLUMN IF NOT EXISTS last_read_message_id BIGINT;

-- last_read_at이 있는 기존 데이터는, 해당 시점까지의 마지막 메시지 ID로 최대한 역산하여 채운다.
-- (정확한 1:1 매칭은 보장 불가하지만, 읽음 기준을 id로 전환하기 위한 합리적인 백필)
UPDATE chat_room_members m
SET last_read_message_id = sub.max_message_id
FROM (
    SELECT
        crm.chat_room_id,
        crm.user_id,
        MAX(msg.id) AS max_message_id
    FROM chat_room_members crm
    JOIN messages msg
      ON msg.chat_room_id = crm.chat_room_id
     AND msg.is_deleted = false
     AND crm.last_read_at IS NOT NULL
     AND msg.created_at <= crm.last_read_at
    GROUP BY crm.chat_room_id, crm.user_id
) sub
WHERE m.chat_room_id = sub.chat_room_id
  AND m.user_id = sub.user_id
  AND m.last_read_message_id IS NULL;

-- 조회/카운팅 성능 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_room_members_last_read_message_id
    ON chat_room_members(chat_room_id, user_id, last_read_message_id);

