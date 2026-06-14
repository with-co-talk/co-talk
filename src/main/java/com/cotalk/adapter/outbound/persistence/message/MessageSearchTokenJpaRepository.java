package com.cotalk.adapter.outbound.persistence.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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
     * 주어진 메시지 ID들 중 검색 토큰이 1개 이상 존재하는 ID를 한 번의 {@code IN} 쿼리로 조회한다.
     * (백필 skip-existing N+1 방지용)
     *
     * <p>청크의 모든 message_id를 한 번에 조회해 청크당 SELECT를 1회로 줄인다.</p>
     *
     * @param messageIds 확인할 메시지 ID 목록
     * @return 토큰이 존재하는 message_id의 distinct 목록
     */
    @Query("SELECT DISTINCT t.messageId FROM MessageSearchTokenJpaEntity t WHERE t.messageId IN :messageIds")
    List<Long> findExistingTokenMessageIds(@Param("messageIds") Collection<Long> messageIds);
}
