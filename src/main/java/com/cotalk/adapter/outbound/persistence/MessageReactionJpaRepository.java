package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionJpaRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<MessageReaction> findByMessageId(Long messageId);

    void deleteByMessageId(Long messageId);
}
