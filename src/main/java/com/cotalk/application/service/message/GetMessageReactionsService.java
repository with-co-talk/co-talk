package com.cotalk.application.service.message;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.GetMessageReactionsUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 메시지 반응 조회 유스케이스 구현체.
 * 메시지에 추가된 반응 목록을 조회한다.
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
     * @return 반응 목록
     */
    @Override
    public List<MessageReaction> getReactions(Long messageId) {
        return reactionRepository.findByMessageId(messageId);
    }
}
