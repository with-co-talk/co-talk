package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 채팅방 presence inactive 요청 DTO.
 * 클라이언트가 방을 "보고 있지 않음" 상태로 전환할 때 호출하여 서버 presence를 비활성화한다.
 *
 * @param roomId 채팅방 ID
 * @param userId 사용자 ID
 */
public record PresenceInactiveRequest(
        Long roomId,
        Long userId
) {}

