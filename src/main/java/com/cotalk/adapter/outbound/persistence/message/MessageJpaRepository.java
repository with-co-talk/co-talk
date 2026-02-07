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
     * @param userId 사용자 ID (본인 메시지 제외)
     * @param lastReadAt 마지막 읽은 시각
     * @return 읽지 않은 메시지 수
     */
    @Query("""
        SELECT COUNT(m)
        FROM Message m
        WHERE m.chatRoomId = :chatRoomId
          AND m.deleted = false
          AND m.senderId <> :userId
          AND (:lastReadAt IS NULL OR m.createdAt > :lastReadAt)
        """)
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("userId") Long userId,
                             @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * 채팅방에서 마지막 읽은 메시지 ID 이후의 읽지 않은 메시지 수를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (본인 메시지 제외)
     * @param lastReadMessageId 마지막 읽은 메시지 ID (null이면 모두 안 읽음)
     * @return 읽지 않은 메시지 수
     */
    @Query("""
        SELECT COUNT(m)
        FROM Message m
        WHERE m.chatRoomId = :chatRoomId
          AND m.deleted = false
          AND m.senderId <> :userId
          AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
        """)
    long countUnreadMessagesByLastReadMessageId(@Param("chatRoomId") Long chatRoomId,
                                                @Param("userId") Long userId,
                                                @Param("lastReadMessageId") Long lastReadMessageId);

    /**
     * 채팅방에서 가장 최근 메시지를 조회한다.
     * 같은 생성 시간을 가진 메시지가 있을 경우 ID로 추가 정렬하여 고유성을 보장한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 가장 최근 메시지 (Optional)
     */
    @Query(value = "SELECT * FROM messages WHERE chat_room_id = :chatRoomId AND is_deleted = false ORDER BY created_at DESC, id DESC LIMIT 1", nativeQuery = true)
    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId);

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

    /**
     * 여러 채팅방의 마지막 메시지를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     * 각 채팅방별로 가장 최근 메시지 1개씩 반환한다.
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @return 마지막 메시지 목록
     */
    @Query(value = """
        SELECT m.* FROM messages m
        INNER JOIN (
            SELECT chat_room_id, MAX(id) as max_id
            FROM messages
            WHERE chat_room_id IN :chatRoomIds AND is_deleted = false
            GROUP BY chat_room_id
        ) latest ON m.id = latest.max_id
        """, nativeQuery = true)
    List<Message> findLastMessagesByRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);

    /**
     * 여러 채팅방의 읽지 않은 메시지 수를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 사용자 ID
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 ID와 읽지 않은 메시지 수 배열의 목록 (Object[0]=chatRoomId, Object[1]=unreadCount)
     */
    @Query(value = """
        SELECT m.chat_room_id as chatRoomId,
               COUNT(m.id) as unreadCount
        FROM messages m
        INNER JOIN chat_room_members crm
            ON crm.chat_room_id = m.chat_room_id AND crm.user_id = :userId
        WHERE m.chat_room_id IN :chatRoomIds
          AND m.is_deleted = false
          AND m.sender_id <> :userId
          AND (crm.last_read_message_id IS NULL OR m.id > crm.last_read_message_id)
        GROUP BY m.chat_room_id
        """, nativeQuery = true)
    List<Object[]> batchCountUnreadMessages(
            @Param("userId") Long userId,
            @Param("chatRoomIds") List<Long> chatRoomIds);

    /**
     * 채팅방에서 특정 사용자를 제외한 다른 발신자 ID 목록을 조회한다.
     * 1:1 채팅방에서 상대방이 나갔을 때 상대방 ID를 찾는 데 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param excludeUserId 제외할 사용자 ID
     * @return 다른 발신자 ID 목록
     */
    @Query("SELECT DISTINCT m.senderId FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.senderId <> :excludeUserId AND m.deleted = false")
    List<Long> findDistinctSenderIdsByChatRoomIdExcludingUser(
            @Param("chatRoomId") Long chatRoomId,
            @Param("excludeUserId") Long excludeUserId);

    /**
     * 채팅방의 모든 멤버에 대해 읽지 않은 메시지 수를 한 번에 조회한다.
     * (N+1 쿼리 방지용 배치 조회)
     *
     * <p>각 멤버의 lastReadMessageId를 기준으로 해당 멤버가 읽지 않은 메시지 수를 계산한다.
     * 본인이 보낸 메시지는 제외한다.</p>
     *
     * @param chatRoomId 채팅방 ID
     * @return 사용자 ID와 읽지 않은 메시지 수 배열의 목록 (Object[0]=userId, Object[1]=unreadCount)
     */
    @Query(value = """
        SELECT cm.user_id as userId,
               COUNT(m.id) as unreadCount
        FROM chat_room_members cm
        LEFT JOIN messages m ON m.chat_room_id = cm.chat_room_id
          AND m.is_deleted = false
          AND m.sender_id <> cm.user_id
          AND (cm.last_read_message_id IS NULL OR m.id > cm.last_read_message_id)
        WHERE cm.chat_room_id = :chatRoomId
        GROUP BY cm.user_id
        """, nativeQuery = true)
    List<Object[]> batchCountUnreadMessagesForAllMembers(@Param("chatRoomId") Long chatRoomId);

    /**
     * 채팅방에서 특정 유형의 메시지를 페이징하여 조회한다.
     * 미디어 갤러리 기능에서 사진/파일 목록을 불러올 때 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param types 메시지 유형 목록 (IMAGE, FILE)
     * @param pageable 페이지 정보
     * @return 해당 유형의 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.type IN :types AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findByTypeInChatRoom(
            @Param("chatRoomId") Long chatRoomId,
            @Param("types") List<Message.MessageType> types,
            Pageable pageable);

    /**
     * 채팅방에서 링크 프리뷰가 있는 텍스트 메시지를 조회한다.
     * 미디어 갤러리의 링크 탭에서 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지 정보
     * @return 링크 프리뷰가 있는 메시지 목록
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.linkPreviewUrl IS NOT NULL AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findMessagesWithLinkPreview(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);
}
