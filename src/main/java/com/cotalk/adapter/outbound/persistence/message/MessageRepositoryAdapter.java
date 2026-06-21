package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.adapter.outbound.persistence.entity.MessageJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.MessageMapper;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 메시지 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;
    private final MessageMapper mapper;

    /**
     * 메시지를 저장한다.
     *
     * @param message 저장할 메시지 엔티티
     * @return 저장된 메시지 엔티티
     */
    @Override
    public Message save(Message message) {
        MessageJpaEntity saved = messageJpaRepository.save(mapper.toJpa(message));
        return mapper.toDomain(saved);
    }

    /**
     * ID로 메시지를 조회한다.
     *
     * @param id 메시지 ID
     * @return 메시지 (Optional)
     */
    @Override
    public Optional<Message> findById(Long id) {
        return messageJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 채팅방 ID로 메시지 목록을 생성일 역순으로 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 메시지 목록
     */
    @Override
    public List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, int page, int size) {
        return messageJpaRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 채팅방에서 읽지 않은 메시지 수를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadAt 마지막 읽은 시각
     * @return 읽지 않은 메시지 수
     */
    @Override
    public long countUnreadMessages(Long chatRoomId, Long userId, LocalDateTime lastReadAt) {
        return messageJpaRepository.countUnreadMessages(chatRoomId, userId, lastReadAt);
    }

    @Override
    public long countUnreadMessagesByLastReadMessageId(Long chatRoomId, Long userId, Long lastReadMessageId) {
        return messageJpaRepository.countUnreadMessagesByLastReadMessageId(chatRoomId, userId, lastReadMessageId);
    }

    /**
     * 채팅방에서 가장 최근 메시지를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 가장 최근 메시지 (Optional)
     */
    @Override
    public Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId) {
        return messageJpaRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId).map(mapper::toDomain);
    }

    /**
     * 특정 메시지 ID 이전의 메시지 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param beforeMessageId 기준 메시지 ID
     * @param size 조회 개수
     * @return 메시지 목록
     */
    @Override
    public List<Message> findByChatRoomIdBeforeMessageId(Long chatRoomId, Long beforeMessageId, int size) {
        List<MessageJpaEntity> result;
        if (beforeMessageId == null) {
            result = messageJpaRepository.findByChatRoomIdOrderByIdDesc(chatRoomId, PageRequest.of(0, size));
        } else {
            result = messageJpaRepository.findByChatRoomIdAndIdLessThan(chatRoomId, beforeMessageId, PageRequest.of(0, size));
        }
        return result.stream().map(mapper::toDomain).toList();
    }

    /**
     * 특정 채팅방에서 블라인드 인덱스 토큰으로 메시지를 검색한다. (1단계)
     *
     * @param chatRoomId 채팅방 ID
     * @param tokens 키워드 토큰 집합
     * @param tokenCount 토큰 개수
     * @param offset 조회 시작 오프셋 (사용자 page 기준)
     * @param limit 조회 윈도우 크기 (over-fetch 한도)
     * @return 토큰 조건을 만족하는 메시지 목록
     */
    @Override
    public List<Message> searchByTokensInChatRoom(Long chatRoomId, List<String> tokens, long tokenCount, long offset, int limit) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return messageJpaRepository.searchByTokensInChatRoom(chatRoomId, tokens, tokenCount, OffsetLimitPageable.of(offset, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자가 참여한 모든 채팅방에서 블라인드 인덱스 토큰으로 메시지를 검색한다. (1단계)
     *
     * @param userId 사용자 ID
     * @param tokens 키워드 토큰 집합
     * @param tokenCount 토큰 개수
     * @param offset 조회 시작 오프셋 (사용자 page 기준)
     * @param limit 조회 윈도우 크기 (over-fetch 한도)
     * @return 토큰 조건을 만족하는 메시지 목록
     */
    @Override
    public List<Message> searchByTokensInUserChatRooms(Long userId, List<String> tokens, long tokenCount, long offset, int limit) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return messageJpaRepository.searchByTokensInUserChatRooms(userId, tokens, tokenCount, OffsetLimitPageable.of(offset, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 전체 메시지 수를 조회한다.
     *
     * @return 메시지 수
     */
    @Override
    public long count() {
        return messageJpaRepository.count();
    }

    /**
     * 여러 채팅방의 마지막 메시지를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @return 마지막 메시지 목록
     */
    @Override
    public List<Message> findLastMessagesByRoomIds(List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return List.of();
        }
        return messageJpaRepository.findLastMessagesByRoomIds(chatRoomIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 여러 채팅방의 읽지 않은 메시지 수를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 사용자 ID
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 ID를 키로, 읽지 않은 메시지 수를 값으로 하는 Map
     */
    @Override
    public Map<Long, Long> batchCountUnreadMessages(Long userId, List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = messageJpaRepository.batchCountUnreadMessages(userId, chatRoomIds);
        Map<Long, Long> unreadCountMap = new HashMap<>();

        for (Object[] row : results) {
            Long chatRoomId = ((Number) row[0]).longValue();
            Long unreadCount = ((Number) row[1]).longValue();
            unreadCountMap.put(chatRoomId, unreadCount);
        }

        // 결과에 없는 채팅방 ID는 0으로 설정
        for (Long chatRoomId : chatRoomIds) {
            unreadCountMap.putIfAbsent(chatRoomId, 0L);
        }

        return unreadCountMap;
    }

    /**
     * 채팅방에서 특정 사용자를 제외한 다른 발신자 ID 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param excludeUserId 제외할 사용자 ID
     * @return 다른 발신자 ID 목록
     */
    @Override
    public List<Long> findDistinctSenderIdsByChatRoomIdExcludingUser(Long chatRoomId, Long excludeUserId) {
        return messageJpaRepository.findDistinctSenderIdsByChatRoomIdExcludingUser(chatRoomId, excludeUserId);
    }

    /**
     * 여러 채팅방에서 특정 사용자를 제외한 다른 발신자 ID를 한 번에 조회한다.
     * 1:1 채팅방에서 상대방이 나갔을 때 상대방 ID를 배치로 찾는 데 사용한다. (N+1 쿼리 방지)
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @param excludeUserId 제외할 사용자 ID
     * @return 채팅방 ID를 키로, 첫 번째 다른 발신자 ID를 값으로 하는 Map
     */
    @Override
    public Map<Long, Long> findDistinctSenderIdsByChatRoomIdsExcludingUser(List<Long> chatRoomIds, Long excludeUserId) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = messageJpaRepository.findDistinctSenderIdsByChatRoomIdsExcludingUser(chatRoomIds, excludeUserId);
        Map<Long, Long> senderIdMap = new HashMap<>();

        for (Object[] row : results) {
            Long chatRoomId = ((Number) row[0]).longValue();
            Long senderId = ((Number) row[1]).longValue();
            // 1:1 채팅방이므로 첫 번째 발신자만 사용 (putIfAbsent)
            senderIdMap.putIfAbsent(chatRoomId, senderId);
        }

        return senderIdMap;
    }

    /**
     * 채팅방의 모든 멤버에 대해 읽지 않은 메시지 수를 한 번에 조회한다.
     * (N+1 쿼리 방지용 배치 조회)
     *
     * @param chatRoomId 채팅방 ID
     * @return 사용자 ID를 키로, 읽지 않은 메시지 수를 값으로 하는 Map
     */
    @Override
    public Map<Long, Long> batchCountUnreadMessagesForAllMembers(Long chatRoomId) {
        List<Object[]> results = messageJpaRepository.batchCountUnreadMessagesForAllMembers(chatRoomId);
        Map<Long, Long> unreadCountMap = new HashMap<>();

        for (Object[] row : results) {
            Long userId = ((Number) row[0]).longValue();
            Long unreadCount = ((Number) row[1]).longValue();
            unreadCountMap.put(userId, unreadCount);
        }

        return unreadCountMap;
    }

    /**
     * 여러 사용자에 대해 모든 채팅방을 합산한 총 읽지 않은 메시지 수를 한 번에 조회한다.
     * (N+1 쿼리 방지용 배치 조회)
     *
     * @param userIds 사용자 ID 목록
     * @return 사용자 ID를 키로, 총 읽지 않은 메시지 수를 값으로 하는 Map (읽지 않은 메시지가 없는 사용자는 0)
     */
    @Override
    public Map<Long, Long> batchCountTotalUnread(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = messageJpaRepository.batchCountTotalUnreadByUserIds(userIds);
        Map<Long, Long> unreadCountMap = new HashMap<>();

        for (Object[] row : results) {
            Long userId = ((Number) row[0]).longValue();
            Long unreadCount = ((Number) row[1]).longValue();
            unreadCountMap.put(userId, unreadCount);
        }

        // 결과에 없는 사용자 ID는 0으로 설정
        for (Long userId : userIds) {
            unreadCountMap.putIfAbsent(userId, 0L);
        }

        return unreadCountMap;
    }

    /**
     * 채팅방에서 특정 타입의 메시지를 조회한다. (미디어 갤러리용)
     *
     * @param chatRoomId 채팅방 ID
     * @param types 메시지 타입 목록 (IMAGE, FILE 등)
     * @param query 페이징 정보
     * @return 해당 타입의 메시지 목록 (최신순)
     */
    @Override
    public List<Message> findByTypeInChatRoom(Long chatRoomId, List<Message.MessageType> types, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size());
        return messageJpaRepository.findByTypeInChatRoom(chatRoomId, types, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 채팅방에서 링크 미리보기가 있는 메시지를 조회한다. (미디어 갤러리용)
     *
     * @param chatRoomId 채팅방 ID
     * @param query 페이징 정보
     * @return 링크 미리보기가 있는 메시지 목록 (최신순)
     */
    @Override
    public List<Message> findMessagesWithLinkPreview(Long chatRoomId, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size());
        return messageJpaRepository.findMessagesWithLinkPreview(chatRoomId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 발신자 ID로 해당 사용자가 보낸 모든 메시지를 삭제한다.
     *
     * @param senderId 발신자(사용자) ID
     */
    @Override
    public void deleteBySenderId(Long senderId) {
        messageJpaRepository.deleteBySenderId(senderId);
    }
}
