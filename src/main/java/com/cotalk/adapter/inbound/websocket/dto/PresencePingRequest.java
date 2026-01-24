package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 채팅방 presence ping 요청 DTO.
 * 클라이언트가 방을 "보고 있는 동안" 주기적으로 보내 서버의 presence TTL을 갱신한다.
 *
 * @param roomId  채팅방 ID
 * @param userId  사용자 ID
 */
public record PresencePingRequest(
        Long roomId,
        Long userId
) {}

