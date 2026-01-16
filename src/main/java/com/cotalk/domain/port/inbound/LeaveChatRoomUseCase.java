package com.cotalk.domain.port.inbound;

/**
 * 채팅방 나가기 유즈케이스
 */
public interface LeaveChatRoomUseCase {

    /**
     * 채팅방에서 나가기
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void leaveChatRoom(Long chatRoomId, Long userId);
}
