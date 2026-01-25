package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 메시지 반응 브로드캐스트 메시지 DTO.
 * <p>
 * Redis Pub/Sub을 통해 모든 서버 인스턴스로 전파되는 반응 이벤트 메시지입니다.
 * WebSocket을 통해 클라이언트에게 전달됩니다.
 *
 * @param reactionId 반응 ID
 * @param messageId  대상 메시지 ID
 * @param userId     반응한 사용자 ID
 * @param emoji      이모지 문자열
 * @param eventType  이벤트 타입 ("ADDED" 또는 "REMOVED")
 * @param timestamp  이벤트 발생 시간 (Unix timestamp, 밀리초)
 * @author seunggu.lee
 */
public record ReactionBroadcastMessage(
        Integer schemaVersion,
        String eventId,
        Long reactionId,
        Long messageId,
        Long userId,
        String emoji,
        String eventType,
        long timestamp
) {}
