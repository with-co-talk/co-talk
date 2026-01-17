package com.cotalk.domain.port.inbound.chatroom;

/**
 * 1:1 채팅방 생성 유스케이스.
 * 두 사용자 간의 1:1 채팅방을 생성한다.
 *
 * @author seunggu.lee
 */
public interface CreateChatRoomUseCase {

    /**
     * 두 사용자 간의 1:1 채팅방을 생성한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 생성된 채팅방 ID
     */
    Long createChatRoom(Long userId1, Long userId2);
}
