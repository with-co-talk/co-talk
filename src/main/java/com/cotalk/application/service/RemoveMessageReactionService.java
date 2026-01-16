package com.cotalk.application.service;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.MessageReactionNotFoundException;
import com.cotalk.domain.port.inbound.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RemoveMessageReactionService implements RemoveMessageReactionUseCase {

    private final MessageReactionRepository reactionRepository;

    @Override
    public void removeReaction(Long messageId, Long userId, String emoji) {
        MessageReaction reaction = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseThrow(() -> new MessageReactionNotFoundException(messageId, userId, emoji));

        reactionRepository.delete(reaction);
        log.info("Message reaction removed: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
    }
}
