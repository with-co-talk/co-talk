package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 반응 추가 유스케이스 구현체.
 * 메시지에 이모지 반응을 추가한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AddMessageReactionService implements AddMessageReactionUseCase {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageValidator messageValidator;

    /**
     * 메시지에 이모지 반응을 추가한다.
     * 이미 같은 반응이 있으면 기존 반응을 반환한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emojiString 이모지 문자열 (이모지 문자 또는 이름)
     * @return 생성된 또는 기존 반응 정보
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     */
    @Override
    public MessageReaction addReaction(Long messageId, Long userId, String emojiString) {
        // 메시지 존재 확인
        messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 이모지 유효성 검증 및 변환
        Emoji emoji = messageValidator.validateAndParseEmoji(emojiString);

        // 이미 같은 반응이 있는지 확인
        return reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseGet(() -> {
                    MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);
                    MessageReaction saved = reactionRepository.save(reaction);
                    log.info("Message reaction added: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
                    return saved;
                });
    }
}
