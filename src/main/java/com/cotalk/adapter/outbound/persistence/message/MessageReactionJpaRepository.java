package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 메시지 리액션 JPA 리포지토리.
 * Spring Data JPA를 통해 메시지 리액션 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface MessageReactionJpaRepository extends JpaRepository<MessageReaction, Long> {

    /**
     * 메시지 ID, 사용자 ID, 이모지로 메시지 리액션을 조회한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 메시지 리액션 (Optional)
     */
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, Emoji emoji);

    /**
     * 메시지 ID로 모든 리액션 목록을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 메시지 리액션 목록
     */
    List<MessageReaction> findByMessageId(Long messageId);

    /**
     * 메시지 ID로 모든 리액션을 삭제한다.
     *
     * @param messageId 메시지 ID
     */
    void deleteByMessageId(Long messageId);
}
