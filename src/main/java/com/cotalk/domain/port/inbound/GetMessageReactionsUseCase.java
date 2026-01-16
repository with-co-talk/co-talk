package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.MessageReaction;

import java.util.List;

/**
 * 메시지 반응 조회 유즈케이스
 */
public interface GetMessageReactionsUseCase {

    /**
     * 메시지의 모든 반응 조회
     *
     * @param messageId 메시지 ID
     * @return 반응 목록
     */
    List<MessageReaction> getReactions(Long messageId);
}
