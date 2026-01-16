package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.MessageReaction;

/**
 * 메시지 반응 추가 유즈케이스
 */
public interface AddMessageReactionUseCase {

    /**
     * 메시지에 반응 추가
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 추가된 반응
     */
    MessageReaction addReaction(Long messageId, Long userId, String emoji);
}
