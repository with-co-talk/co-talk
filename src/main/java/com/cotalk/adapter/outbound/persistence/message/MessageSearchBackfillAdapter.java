package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.MessageSearchBackfillPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 기존 메시지 검색 토큰 백필용 영속성 어댑터 (PR2).
 *
 * <p>{@link MessageSearchBackfillPort}를 JPA로 구현한다. {@code id} 커서 청크 조회와
 * 토큰 존재 여부 확인을 제공한다.</p>
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class MessageSearchBackfillAdapter implements MessageSearchBackfillPort {

    private final MessageJpaRepository messageJpaRepository;
    private final MessageSearchTokenJpaRepository messageSearchTokenJpaRepository;

    /**
     * TEXT(미삭제) 메시지를 id 오름차순 커서 청크로 조회한다.
     *
     * @param afterId   커서 (이 ID보다 큰 메시지)
     * @param chunkSize 청크 크기
     * @return id 오름차순 메시지 목록
     */
    @Override
    public List<Message> findTextMessagesForBackfill(long afterId, int chunkSize) {
        return messageJpaRepository.findTextMessagesForBackfill(afterId, PageRequest.of(0, chunkSize));
    }

    /**
     * 메시지에 검색 토큰이 이미 존재하는지 확인한다.
     *
     * @param messageId 메시지 ID
     * @return 토큰이 존재하면 {@code true}
     */
    @Override
    public boolean hasTokens(long messageId) {
        return messageSearchTokenJpaRepository.existsByMessageId(messageId);
    }
}
