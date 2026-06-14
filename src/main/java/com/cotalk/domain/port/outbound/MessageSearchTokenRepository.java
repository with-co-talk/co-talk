package com.cotalk.domain.port.outbound;

import java.util.Set;

/**
 * 메시지 검색 토큰 레포지토리 포트.
 *
 * <p>블라인드 인덱스 토큰({@code message_search_tokens})의 저장/삭제를 담당한다.
 * 검색 조회 자체는 {@link MessageRepository}의 토큰 조인 검색 메서드가 수행하며,
 * 이 포트는 적재(write) 경로만 책임진다.</p>
 *
 * @author seunggu.lee
 */
public interface MessageSearchTokenRepository {

    /**
     * 메시지의 검색 토큰들을 저장한다.
     *
     * <p>같은 트랜잭션 내에서 메시지 저장 직후 호출되어 부분 저장을 방지한다.
     * 빈 토큰 집합이면 아무 것도 하지 않는다.</p>
     *
     * @param messageId 토큰이 속한 메시지 ID
     * @param tokens    저장할 토큰 집합
     */
    void saveTokens(Long messageId, Set<String> tokens);

    /**
     * 특정 메시지의 모든 검색 토큰을 삭제한다.
     *
     * <p>메시지 수정 시 재토큰화(delete-then-insert)에 사용한다.</p>
     *
     * @param messageId 토큰을 삭제할 메시지 ID
     */
    void deleteByMessageId(Long messageId);
}
