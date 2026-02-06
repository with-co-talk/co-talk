package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 채팅방 이름 변경 요청 DTO.
 *
 * @param newName 새 채팅방 이름
 * @author seunggu.lee
 */
public record UpdateChatRoomNameRequest(
        @NotBlank(message = "새 채팅방 이름은 필수입니다.")
        @Size(max = 50, message = "채팅방 이름은 50자를 초과할 수 없습니다.")
        String newName
) {

    /**
     * 채팅방 이름 변경 요청을 생성합니다.
     *
     * @param newName 새 채팅방 이름
     * @return UpdateChatRoomNameRequest 인스턴스
     */
    public static UpdateChatRoomNameRequest of(String newName) {
        return new UpdateChatRoomNameRequest(newName);
    }
}
