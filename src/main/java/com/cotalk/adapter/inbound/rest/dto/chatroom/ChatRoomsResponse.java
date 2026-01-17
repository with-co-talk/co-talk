package com.cotalk.adapter.inbound.rest.dto.chatroom;

import java.util.List;

/**
 * 채팅방 목록 응답 DTO.
 *
 * @param rooms 채팅방 목록
 * @author seunggu.lee
 */
public record ChatRoomsResponse(List<ChatRoomDto> rooms) {

    /**
     * 채팅방 목록 응답을 생성합니다.
     *
     * @param rooms 채팅방 목록
     * @return ChatRoomsResponse 인스턴스
     */
    public static ChatRoomsResponse of(List<ChatRoomDto> rooms) {
        return new ChatRoomsResponse(rooms);
    }
}
