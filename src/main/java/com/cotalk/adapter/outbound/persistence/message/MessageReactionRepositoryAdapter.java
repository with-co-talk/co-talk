package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 메시지 리액션 영속성 어댑터.
 * JPA를 통해 메시지 리액션 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class MessageReactionRepositoryAdapter implements MessageReactionRepository {

    private final MessageReactionJpaRepository jpaRepository;

    /**
     * 메시지 리액션을 저장한다.
     *
     * @param reaction 저장할 메시지 리액션 엔티티
     * @return 저장된 메시지 리액션 엔티티
     */
    @Override
    public MessageReaction save(MessageReaction reaction) {
        return jpaRepository.save(reaction);
    }

    /**
     * 메시지 ID, 사용자 ID, 이모지로 메시지 리액션을 조회한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 메시지 리액션 (Optional)
     */
    @Override
    public Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, com.cotalk.domain.entity.Emoji emoji) {
        return jpaRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
    }

    /**
     * 메시지 ID로 모든 리액션 목록을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 메시지 리액션 목록
     */
    @Override
    public List<MessageReaction> findByMessageId(Long messageId) {
        return jpaRepository.findByMessageId(messageId);
    }

    /**
     * 메시지 리액션을 삭제한다.
     *
     * @param reaction 삭제할 메시지 리액션 엔티티
     */
    @Override
    public void delete(MessageReaction reaction) {
        jpaRepository.delete(reaction);
    }

    /**
     * 메시지 ID로 모든 리액션을 삭제한다.
     *
     * @param messageId 메시지 ID
     */
    @Override
    public void deleteByMessageId(Long messageId) {
        jpaRepository.deleteByMessageId(messageId);
    }

    /**
     * 사용자 ID로 해당 사용자가 남긴 모든 리액션을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    /**
     * 특정 발신자가 보낸 메시지에 달린 모든 리액션을 삭제한다.
     *
     * @param senderId 메시지 발신자(사용자) ID
     */
    @Override
    public void deleteByMessageSenderId(Long senderId) {
        jpaRepository.deleteByMessageSenderId(senderId);
    }
}
