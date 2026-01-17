package com.cotalk.domain.port.inbound.chatroom;

/**
 * 채팅방 나가기 유스케이스.
 * 사용자가 채팅방에서 나가는 기능을 제공한다.
 *
 * @author seunggu.lee
 */
public interface LeaveChatRoomUseCase {

    /**
     * 채팅방에서 나간다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void leaveChatRoom(Long chatRoomId, Long userId);
}
