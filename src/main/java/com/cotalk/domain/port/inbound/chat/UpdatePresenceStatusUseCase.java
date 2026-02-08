package com.cotalk.domain.port.inbound.chat;

/**
 * 채팅방 presence 상태 변경 유스케이스.
 * 사용자가 채팅방을 보고 있는지(활성/비활성) 상태를 관리한다.
 *
 * @author seunggu.lee
 */
public interface UpdatePresenceStatusUseCase {

    /**
     * 사용자를 채팅방 활성 상태로 마킹한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param sessionId  WebSocket 세션 ID
     */
    void markActive(Long chatRoomId, Long userId, String sessionId);

    /**
     * 사용자를 채팅방 비활성 상태로 마킹한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param sessionId  WebSocket 세션 ID
     */
    void markInactive(Long chatRoomId, Long userId, String sessionId);
}
