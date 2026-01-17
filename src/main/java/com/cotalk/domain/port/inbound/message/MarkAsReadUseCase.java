package com.cotalk.domain.port.inbound.message;

/**
 * 읽음 표시 유스케이스.
 * 채팅방의 메시지를 읽음으로 표시한다.
 *
 * @author seunggu.lee
 */
public interface MarkAsReadUseCase {

    /**
     * 채팅방의 메시지를 읽음으로 표시한다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     */
    void markAsRead(Long userId, Long chatRoomId);
}
