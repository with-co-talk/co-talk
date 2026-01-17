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
}
