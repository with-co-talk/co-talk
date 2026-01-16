package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.MessageReaction;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository {

    MessageReaction save(MessageReaction reaction);

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<MessageReaction> findByMessageId(Long messageId);

    void delete(MessageReaction reaction);

    void deleteByMessageId(Long messageId);
}
