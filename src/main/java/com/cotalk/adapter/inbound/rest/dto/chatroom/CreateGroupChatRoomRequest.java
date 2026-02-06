package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 그룹 채팅방 생성 요청 DTO.
 *
 * @param roomName  채팅방 이름
 * @param memberIds 초기 멤버 ID 목록
 * @author seunggu.lee
 */
public record CreateGroupChatRoomRequest(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        @Size(max = 50, message = "채팅방 이름은 50자를 초과할 수 없습니다.")
        String roomName,

        @NotNull(message = "멤버 목록은 필수입니다.")
        List<Long> memberIds
) {

    /**
     * 그룹 채팅방 생성 요청을 생성합니다.
     *
     * @param roomName  채팅방 이름
     * @param memberIds 초기 멤버 ID 목록
     * @return CreateGroupChatRoomRequest 인스턴스
     */
    public static CreateGroupChatRoomRequest of(String roomName, List<Long> memberIds) {
        return new CreateGroupChatRoomRequest(roomName, memberIds);
    }
}
