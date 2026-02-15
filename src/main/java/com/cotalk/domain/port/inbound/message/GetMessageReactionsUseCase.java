package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.model.GroupedReaction;

import java.util.List;

/**
 * 메시지 반응 조회 유스케이스.
 * 메시지에 추가된 반응 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetMessageReactionsUseCase {

    /**
     * 메시지의 모든 반응을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 반응 목록
     */
    List<MessageReaction> getReactions(Long messageId);

    /**
     * 메시지의 반응을 이모지별로 그룹핑하여 조회한다.
     *
     * @param messageId     메시지 ID
     * @param currentUserId 현재 사용자 ID (null 가능)
     * @return 그룹핑된 반응 목록
     */
    List<GroupedReaction> getGroupedReactions(Long messageId, Long currentUserId);
}
