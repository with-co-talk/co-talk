package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.model.GroupedReaction;
import com.cotalk.domain.port.inbound.message.GetMessageReactionsUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 메시지 반응 조회 유스케이스 구현체.
 * 메시지에 추가된 반응 목록을 이모지별로 그룹핑하여 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageReactionsService implements GetMessageReactionsUseCase {

    private final MessageReactionRepository reactionRepository;

    /**
     * 메시지의 반응 목록을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 반응 목록 (개별 레코드)
     */
    @Override
    public List<MessageReaction> getReactions(Long messageId) {
        return reactionRepository.findByMessageId(messageId);
    }

    /**
     * 메시지의 반응을 이모지별로 그룹핑하여 조회한다.
     *
     * @param messageId    메시지 ID
     * @param currentUserId 현재 사용자 ID (null 가능)
     * @return 그룹핑된 반응 목록
     */
    @Override
    public List<GroupedReaction> getGroupedReactions(Long messageId, Long currentUserId) {
        List<MessageReaction> reactions = reactionRepository.findByMessageId(messageId);

        // 이모지별로 그룹핑
        Map<Emoji, List<Long>> groupedByEmoji = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getEmoji,
                        Collectors.mapping(MessageReaction::getUserId, Collectors.toList())
                ));

        // GroupedReaction으로 변환하고 count 내림차순 정렬
        return groupedByEmoji.entrySet().stream()
                .map(entry -> GroupedReaction.of(entry.getKey(), entry.getValue(), currentUserId))
                .sorted(Comparator.comparing(GroupedReaction::count).reversed())
                .toList();
    }
}
