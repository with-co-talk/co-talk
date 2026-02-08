package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 타이핑 상태 요청 DTO.
 * 클라이언트가 타이핑 시작/중지를 서버에 알린다.
 *
 * @param roomId   채팅방 ID
 * @param isTyping 타이핑 중 여부
 */
public record TypingStatusRequest(
        Long roomId,
        Boolean isTyping
) {}
