package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageReactionRepositoryAdapter implements MessageReactionRepository {

    private final MessageReactionJpaRepository jpaRepository;

    @Override
    public MessageReaction save(MessageReaction reaction) {
        return jpaRepository.save(reaction);
    }

    @Override
    public Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji) {
        return jpaRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
    }

    @Override
    public List<MessageReaction> findByMessageId(Long messageId) {
        return jpaRepository.findByMessageId(messageId);
    }

    @Override
    public void delete(MessageReaction reaction) {
        jpaRepository.delete(reaction);
    }

    @Override
    public void deleteByMessageId(Long messageId) {
        jpaRepository.deleteByMessageId(messageId);
    }
}
