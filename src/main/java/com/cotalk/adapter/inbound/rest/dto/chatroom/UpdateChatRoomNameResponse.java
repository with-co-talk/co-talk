package com.cotalk.adapter.inbound.rest.dto.chatroom;

/**
 * 채팅방 이름 변경 응답 DTO.
 *
 * @param name    변경된 이름
 * @param message 결과 메시지
 * @author seunggu.lee
 */
public record UpdateChatRoomNameResponse(String name, String message) {

    /**
     * 채팅방 이름 변경 응답을 생성합니다.
     *
     * @param name    변경된 이름
     * @param message 결과 메시지
     * @return UpdateChatRoomNameResponse 인스턴스
     */
    public static UpdateChatRoomNameResponse of(String name, String message) {
        return new UpdateChatRoomNameResponse(name, message);
    }
}
