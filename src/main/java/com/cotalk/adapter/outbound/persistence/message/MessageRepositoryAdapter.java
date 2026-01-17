package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 메시지 영속성 어댑터.
 * JPA를 통해 메시지 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;

    /**
     * 메시지를 저장한다.
     *
     * @param message 저장할 메시지 엔티티
     * @return 저장된 메시지 엔티티
     */
    @Override
    public Message save(Message message) {
        return messageJpaRepository.save(message);
    }

    /**
     * ID로 메시지를 조회한다.
     *
     * @param id 메시지 ID
     * @return 메시지 (Optional)
     */
    @Override
    public Optional<Message> findById(Long id) {
        return messageJpaRepository.findById(id);
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
        return messageJpaRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(page, size));
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
        if (lastReadAt == null) {
            return 0;
        }
        return messageJpaRepository.countUnreadMessages(chatRoomId, lastReadAt);
    }

    /**
     * 채팅방에서 가장 최근 메시지를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 가장 최근 메시지 (Optional)
     */
    @Override
    public Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId) {
        return messageJpaRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
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
        if (beforeMessageId == null) {
            return messageJpaRepository.findByChatRoomIdOrderByIdDesc(chatRoomId, PageRequest.of(0, size));
        }
        return messageJpaRepository.findByChatRoomIdAndIdLessThan(chatRoomId, beforeMessageId, PageRequest.of(0, size));
    }

    /**
     * 특정 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록
     */
    @Override
    public List<Message> searchByKeywordInChatRoom(Long chatRoomId, String keyword, int page, int size) {
        return messageJpaRepository.searchByKeywordInChatRoom(chatRoomId, keyword, PageRequest.of(page, size));
    }

    /**
     * 사용자가 참여한 모든 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 메시지 목록
     */
    @Override
    public List<Message> searchByKeywordInUserChatRooms(Long userId, String keyword, int page, int size) {
        return messageJpaRepository.searchByKeywordInUserChatRooms(userId, keyword, PageRequest.of(page, size));
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
}
