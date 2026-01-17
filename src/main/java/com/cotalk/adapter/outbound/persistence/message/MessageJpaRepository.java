package com.cotalk.adapter.outbound.persistence.message;

import com.cotalk.domain.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 메시지 JPA 리포지토리.
 * Spring Data JPA를 통해 메시지 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface MessageJpaRepository extends JpaRepository<Message, Long> {

    /**
     * 채팅방 ID로 메시지 목록을 생성일 역순으로 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지 정보
     * @return 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * 채팅방에서 마지막 읽은 시각 이후의 읽지 않은 메시지 수를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param lastReadAt 마지막 읽은 시각
     * @return 읽지 않은 메시지 수
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.createdAt > :lastReadAt AND m.deleted = false")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * 채팅방에서 가장 최근 메시지를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 가장 최근 메시지 (Optional)
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.createdAt DESC")
    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    /**
     * 특정 메시지 ID 이전의 메시지 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param beforeMessageId 기준 메시지 ID
     * @param pageable 페이지 정보
     * @return 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.id < :beforeMessageId AND m.deleted = false ORDER BY m.id DESC")
    List<Message> findByChatRoomIdAndIdLessThan(
            @Param("chatRoomId") Long chatRoomId,
            @Param("beforeMessageId") Long beforeMessageId,
            Pageable pageable);

    /**
     * 채팅방 ID로 메시지 목록을 ID 역순으로 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지 정보
     * @return 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.id DESC")
    List<Message> findByChatRoomIdOrderByIdDesc(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);

    /**
     * 특정 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param keyword 검색 키워드
     * @param pageable 페이지 정보
     * @return 검색된 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchByKeywordInChatRoom(
            @Param("chatRoomId") Long chatRoomId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 사용자가 참여한 모든 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param pageable 페이지 정보
     * @return 검색된 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId IN (SELECT cm.chatRoomId FROM ChatRoomMember cm WHERE cm.userId = :userId) AND m.deleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchByKeywordInUserChatRooms(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
