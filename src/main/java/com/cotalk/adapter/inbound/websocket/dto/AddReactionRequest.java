package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 메시지 반응 추가 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 반응 추가 요청입니다.
 *
 * @param messageId 반응을 추가할 메시지 ID
 * @param userId    반응을 추가하는 사용자 ID
 * @param emoji     이모지 문자열
 * @author seunggu.lee
 */
public record AddReactionRequest(
        Long messageId,
        Long userId,
        String emoji
) {}
