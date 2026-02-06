package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 텍스트 채팅 메시지 전송 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 텍스트 메시지 요청입니다.
 *
 * @param roomId   채팅방 ID
 * @param content  메시지 내용
 * @author seunggu.lee
 */
public record ChatMessageRequest(
        Long roomId,
        String content
) {}
