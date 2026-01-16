package com.cotalk.application.service;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.AddMessageReactionUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AddMessageReactionService implements AddMessageReactionUseCase {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;

    private static final int MAX_EMOJI_LENGTH = 50;

    @Override
    public MessageReaction addReaction(Long messageId, Long userId, String emoji) {
        // 메시지 존재 확인
        messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 이모지 유효성 검증
        validateEmoji(emoji);

        // 이미 같은 반응이 있는지 확인
        return reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseGet(() -> {
                    MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);
                    MessageReaction saved = reactionRepository.save(reaction);
                    log.info("Message reaction added: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
                    return saved;
                });
    }

    private void validateEmoji(String emoji) {
        if (emoji == null || emoji.trim().isEmpty()) {
            throw InvalidEmojiException.invalidFormat(emoji);
        }
        if (emoji.length() > MAX_EMOJI_LENGTH) {
            throw InvalidEmojiException.tooLong(emoji);
        }
    }
}
