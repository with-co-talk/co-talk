package com.cotalk.adapter.inbound.rest.dto.chatroom;

/**
 * 채팅방 생성 응답 DTO.
 *
 * @param roomId  생성된 채팅방 ID
 * @param message 결과 메시지
 * @author seunggu.lee
 */
public record CreateChatRoomResponse(Long roomId, String message) {

    /**
     * 채팅방 생성 응답을 생성합니다.
     *
     * @param roomId  생성된 채팅방 ID
     * @param message 결과 메시지
     * @return CreateChatRoomResponse 인스턴스
     */
    public static CreateChatRoomResponse of(Long roomId, String message) {
        return new CreateChatRoomResponse(roomId, message);
    }
}
