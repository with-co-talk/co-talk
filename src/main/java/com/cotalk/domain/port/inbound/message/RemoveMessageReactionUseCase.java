package com.cotalk.domain.port.inbound.message;

/**
 * 메시지 반응 제거 유스케이스.
 * 메시지에 추가한 이모지 반응을 제거한다.
 *
 * @author seunggu.lee
 */
public interface RemoveMessageReactionUseCase {

    /**
     * 메시지 반응을 제거한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     */
    void removeReaction(Long messageId, Long userId, String emoji);

    /**
     * 메시지 반응을 제거하고, 브로드캐스트에 필요한 chatRoomId를 반환한다.
     * WebSocket 컨트롤러에서 추가 DB 쿼리 없이 반응 이벤트를 발행할 수 있도록 한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return 메시지가 속한 채팅방 ID (메시지를 찾을 수 없는 경우 null)
     */
    Long removeReactionWithContext(Long messageId, Long userId, String emoji);
}
