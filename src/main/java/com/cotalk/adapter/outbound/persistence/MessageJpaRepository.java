package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 메시지 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 메시지 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface MessageJpaRepository extends JpaRepository<Message, UUID> {
    
    /**
     * 채팅방의 메시지를 최신순으로 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지 정보
     * @return 메시지 목록 (최신순)
     */
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(UUID chatRoomId, Pageable pageable);

    /**
     * 채팅방의 특정 시간 이전 메시지를 최신순으로 조회합니다.
     * 
     * <p>무한 스크롤을 위한 커서 기반 페이지네이션에 사용됩니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param before 기준 시간 (이 시간 이전의 메시지 조회)
     * @param pageable 페이지 정보
     * @return 메시지 목록 (최신순)
     */
    List<Message> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID chatRoomId, LocalDateTime before, Pageable pageable);

    /**
     * 읽지 않은 메시지 수를 계산합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadAt 마지막 읽은 시간
     * @return 읽지 않은 메시지 수
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoomId = :chatRoomId " +
           "AND m.senderId != :userId AND m.createdAt > :lastReadAt")
    long countUnreadMessages(
            @Param("chatRoomId") UUID chatRoomId,
            @Param("userId") UUID userId,
            @Param("lastReadAt") LocalDateTime lastReadAt);
}
