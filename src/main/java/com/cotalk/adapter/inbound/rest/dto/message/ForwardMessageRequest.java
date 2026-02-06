package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotNull;

/**
 * 메시지 전달 요청 DTO.
 *
 * @param targetChatRoomId 전달 대상 채팅방 ID
 * @author seunggu.lee
 */
public record ForwardMessageRequest(
        @NotNull(message = "대상 채팅방 ID는 필수입니다.")
        Long targetChatRoomId
) {

    /**
     * 메시지 전달 요청을 생성합니다.
     *
     * @param targetChatRoomId 전달 대상 채팅방 ID
     * @return ForwardMessageRequest 인스턴스
     */
    public static ForwardMessageRequest of(Long targetChatRoomId) {
        return new ForwardMessageRequest(targetChatRoomId);
    }
}
