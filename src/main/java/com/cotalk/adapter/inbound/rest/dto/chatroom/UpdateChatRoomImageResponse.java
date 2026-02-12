package com.cotalk.adapter.inbound.rest.dto.chatroom;

/**
 * 채팅방 이미지 변경 응답 DTO.
 *
 * @param imageUrl 변경된 이미지 URL
 * @param message 응답 메시지
 * @author seunggu.lee
 */
public record UpdateChatRoomImageResponse(
        String imageUrl,
        String message
) {
    /**
     * 응답 DTO를 생성한다.
     *
     * @param imageUrl 변경된 이미지 URL
     * @param message 응답 메시지
     * @return UpdateChatRoomImageResponse 인스턴스
     */
    public static UpdateChatRoomImageResponse of(String imageUrl, String message) {
        return new UpdateChatRoomImageResponse(imageUrl, message);
    }
}
