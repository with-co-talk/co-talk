package com.cotalk.domain.port.outbound;

/**
 * 채팅방 활성(현재 보고 있는 방) 상태 추적 포트.
 *
 * <p>카톡/라인 스타일의 "상대가 방에 들어와 있으면 즉시 읽음(0)" 처리를 위해,
 * 서버가 특정 사용자가 특정 채팅방을 현재 보고 있는지(활성 상태) 판단할 수 있어야 한다.</p>
 *
 * <p>구현체는 Redis 등을 사용하여 분산 환경에서도 일관되게 동작해야 한다.</p>
 */
public interface ChatRoomPresenceTracker {

    /**
     * 사용자가 채팅방을 "활성" 상태로 진입/구독했음을 기록한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param sessionId  WebSocket 세션 ID
     */
    void markActive(Long chatRoomId, Long userId, String sessionId);

    /**
     * 사용자가 채팅방 활성 상태에서 이탈(구독 해제)했음을 기록한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param sessionId  WebSocket 세션 ID
     */
    void markInactive(Long chatRoomId, Long userId, String sessionId);

    /**
     * 사용자의 WebSocket 세션이 종료되었음을 기록하고, 해당 세션이 활성화했던 채팅방들을 정리한다.
     *
     * @param userId    사용자 ID
     * @param sessionId WebSocket 세션 ID
     */
    void clearSession(Long userId, String sessionId);

    /**
     * 특정 사용자가 특정 채팅방을 현재 활성 상태로 보고 있는지 확인한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return 활성 상태 여부
     */
    boolean isActive(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방에 현재 활성 상태인 사용자 수를 반환한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 활성 사용자 수
     */
    int countActiveMembers(Long chatRoomId);
}

