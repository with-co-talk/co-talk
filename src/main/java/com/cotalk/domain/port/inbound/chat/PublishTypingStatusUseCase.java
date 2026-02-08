package com.cotalk.domain.port.inbound.chat;

/**
 * 타이핑 상태 발행 유스케이스.
 * 사용자의 타이핑 시작/중지 상태를 채팅방 참여자들에게 브로드캐스트한다.
 *
 * @author seunggu.lee
 */
public interface PublishTypingStatusUseCase {

    /**
     * 타이핑 상태를 발행한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param isTyping   타이핑 여부
     */
    void publishTypingStatus(Long chatRoomId, Long userId, boolean isTyping);
}
