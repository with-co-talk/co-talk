package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotNull;

/**
 * 1:1 채팅방 생성 요청 DTO.
 *
 * @param userId2 상대방 사용자 ID
 * @author seunggu.lee
 */
public record CreateChatRoomRequest(
        @NotNull(message = "상대방 사용자 ID는 필수입니다.")
        Long userId2
) {

    /**
     * 1:1 채팅방 생성 요청을 생성합니다.
     *
     * @param userId2 상대방 사용자 ID
     * @return CreateChatRoomRequest 인스턴스
     */
    public static CreateChatRoomRequest of(Long userId2) {
        return new CreateChatRoomRequest(userId2);
    }
}
