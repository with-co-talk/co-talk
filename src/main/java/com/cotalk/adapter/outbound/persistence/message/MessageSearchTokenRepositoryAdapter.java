package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.port.outbound.MessageSearchTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 메시지 검색 토큰 영속성 어댑터.
 *
 * <p>{@link MessageSearchTokenRepository} 포트를 JPA로 구현한다. 토큰 적재/삭제만 담당하며,
 * 검색 조회는 {@code MessageRepository}의 토큰 조인 메서드가 수행한다.</p>
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class MessageSearchTokenRepositoryAdapter implements MessageSearchTokenRepository {

    private final MessageSearchTokenJpaRepository jpaRepository;

    /**
     * 메시지의 검색 토큰들을 저장한다.
     *
     * @param messageId 토큰이 속한 메시지 ID
     * @param tokens    저장할 토큰 집합
     */
    @Override
    public void saveTokens(Long messageId, Set<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        List<MessageSearchTokenJpaEntity> entities = tokens.stream()
                .map(token -> MessageSearchTokenJpaEntity.of(messageId, token))
                .toList();
        jpaRepository.saveAll(entities);
    }

    /**
     * 특정 메시지의 모든 검색 토큰을 삭제한다.
     *
     * @param messageId 토큰을 삭제할 메시지 ID
     */
    @Override
    public void deleteByMessageId(Long messageId) {
        jpaRepository.deleteByMessageId(messageId);
    }
}
