package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 메시지 반응 제거 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 반응 제거 요청입니다.
 *
 * @param messageId 반응을 제거할 메시지 ID
 * @param emoji     제거할 이모지 문자열
 * @author seunggu.lee
 */
public record RemoveReactionRequest(
        Long messageId,
        String emoji
) {}
