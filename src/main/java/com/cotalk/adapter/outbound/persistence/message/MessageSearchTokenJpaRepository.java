package com.cotalk.adapter.outbound.persistence.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 메시지 검색 토큰 JPA 리포지토리.
 *
 * @author seunggu.lee
 */
public interface MessageSearchTokenJpaRepository
        extends JpaRepository<MessageSearchTokenJpaEntity, MessageSearchTokenJpaEntity.MessageSearchTokenId> {

    /**
     * 특정 메시지의 모든 검색 토큰을 삭제한다. (메시지 수정 시 재토큰화용)
     *
     * @param messageId 메시지 ID
     */
    @Modifying
    @Query("DELETE FROM MessageSearchTokenJpaEntity t WHERE t.messageId = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);

    /**
     * 특정 메시지에 검색 토큰이 1개 이상 존재하는지 확인한다. (백필 skip-existing 최적화용)
     *
     * @param messageId 메시지 ID
     * @return 토큰이 존재하면 {@code true}
     */
    boolean existsByMessageId(Long messageId);
}
