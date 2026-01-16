package com.cotalk.domain.port.inbound;

/**
 * 메시지 반응 제거 유즈케이스
 */
public interface RemoveMessageReactionUseCase {

    /**
     * 메시지 반응 제거
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     */
    void removeReaction(Long messageId, Long userId, String emoji);
}
