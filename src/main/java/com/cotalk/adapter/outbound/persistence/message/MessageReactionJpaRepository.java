package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.adapter.outbound.persistence.entity.MessageReactionJpaEntity;
import com.cotalk.domain.entity.Emoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 메시지 리액션 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface MessageReactionJpaRepository extends JpaRepository<MessageReactionJpaEntity, Long> {

    /**
     * 메시지 ID, 사용자 ID, 이모지로 메시지 리액션을 조회한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 메시지 리액션 (Optional)
     */
    Optional<MessageReactionJpaEntity> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, Emoji emoji);

    /**
     * 메시지 ID로 모든 리액션 목록을 조회한다.
     *
     * @param messageId 메시지 ID
     * @return 메시지 리액션 목록
     */
    List<MessageReactionJpaEntity> findByMessageId(Long messageId);

    /**
     * 메시지 ID로 모든 리액션을 삭제한다.
     *
     * @param messageId 메시지 ID
     */
    void deleteByMessageId(Long messageId);

    /**
     * 사용자 ID로 해당 사용자가 남긴 모든 리액션을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 발신자가 보낸 메시지에 달린 모든 리액션을 삭제한다.
     * 메시지를 물리적으로 삭제하기 전에, 해당 메시지를 참조하는 리액션의
     * 외래키 제약 위반을 방지하기 위해 사용한다.
     *
     * <p>벌크 삭제 JPQL은 1차 캐시(영속성 컨텍스트)를 우회하므로,
     * 같은 트랜잭션 내 후속 작업이 삭제된 리액션을 stale 상태로 읽지 않도록
     * {@code flushAutomatically}/{@code clearAutomatically}로 실행 전 flush, 실행 후 clear를 보장한다.</p>
     *
     * @param senderId 메시지 발신자(사용자) ID
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MessageReactionJpaEntity r WHERE r.messageId IN " +
           "(SELECT m.id FROM MessageJpaEntity m WHERE m.senderId = :senderId)")
    void deleteByMessageSenderId(@Param("senderId") Long senderId);
}
