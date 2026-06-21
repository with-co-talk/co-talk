package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.adapter.outbound.persistence.entity.ChatRoomMemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMemberJpaEntity, Long> {

    /**
     * 채팅방 ID와 사용자 ID로 채팅방 멤버를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 멤버 (Optional)
     */
    Optional<ChatRoomMemberJpaEntity> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 채팅방 ID로 모든 멤버 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMemberJpaEntity> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자 ID로 참여 중인 채팅방 멤버 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMemberJpaEntity> findByUserId(Long userId);

    /**
     * 채팅방에 해당 사용자가 멤버로 존재하는지 확인한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 멤버 존재 여부
     */
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 사용자 ID로 모든 채팅방 멤버 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 마지막 읽은 시간을 원자적으로 업데이트한다.
     * 기존 값보다 큰 경우에만 업데이트하여 Lost Update를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 새로운 읽은 시간
     * @return 업데이트된 행 수
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ChatRoomMemberJpaEntity m SET m.lastReadAt = :lastReadAt " +
           "WHERE m.chatRoomId = :chatRoomId AND m.userId = :userId " +
           "AND (m.lastReadAt IS NULL OR m.lastReadAt < :lastReadAt)")
    int updateLastReadAtIfNewer(@Param("chatRoomId") Long chatRoomId,
                                @Param("userId") Long userId,
                                @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * 마지막 읽은 메시지 ID를 원자적으로 업데이트한다.
     * 기존 값보다 큰 경우에만 업데이트하여 Lost Update를 방지한다.
     *
     * <p>lastReadAt은 보조 정보로 함께 갱신한다.</p>
     *
     * <p>lastReadMessageId가 null이 아닌 경우에만 업데이트한다.</p>
     *
     * <p>flushAutomatically=true: 같은 트랜잭션 내에서 후속 쿼리가 업데이트된 데이터를 읽을 수 있도록 함</p>
     * <p>clearAutomatically=true: 영속성 컨텍스트를 클리어하여 캐시된 이전 값 대신 DB의 최신 값을 읽도록 함</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ChatRoomMemberJpaEntity m " +
           "SET m.lastReadMessageId = :lastReadMessageId, m.lastReadAt = :lastReadAt " +
           "WHERE m.chatRoomId = :chatRoomId AND m.userId = :userId " +
           "AND :lastReadMessageId IS NOT NULL " +
           "AND (m.lastReadMessageId IS NULL OR m.lastReadMessageId < :lastReadMessageId)")
    int updateLastReadMessageIdIfNewer(@Param("chatRoomId") Long chatRoomId,
                                       @Param("userId") Long userId,
                                       @Param("lastReadAt") LocalDateTime lastReadAt,
                                       @Param("lastReadMessageId") Long lastReadMessageId);

    /**
     * 특정 메시지를 읽지 않은 멤버 수를 조회한다.
     * 메시지 생성 시간보다 lastReadAt이 이전이거나 null인 멤버의 수를 반환한다.
     * 발신자는 제외한다.
     *
     * @param chatRoomId       채팅방 ID
     * @param messageCreatedAt 메시지 생성 시간
     * @param senderId         발신자 ID (제외할 사용자)
     * @return 읽지 않은 멤버 수
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMemberJpaEntity m " +
           "WHERE m.chatRoomId = :chatRoomId " +
           "AND m.userId != :senderId " +
           "AND (m.lastReadAt IS NULL OR m.lastReadAt < :messageCreatedAt)")
    int countUnreadMembers(@Param("chatRoomId") Long chatRoomId,
                           @Param("messageCreatedAt") LocalDateTime messageCreatedAt,
                           @Param("senderId") Long senderId);

    /**
     * 특정 메시지를 읽지 않은 멤버 수를 조회한다. (messageId 기준)
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMemberJpaEntity m " +
           "WHERE m.chatRoomId = :chatRoomId " +
           "AND m.userId != :senderId " +
           "AND (m.lastReadMessageId IS NULL OR m.lastReadMessageId < :messageId)")
    int countUnreadMembersByMessageId(@Param("chatRoomId") Long chatRoomId,
                                      @Param("messageId") Long messageId,
                                      @Param("senderId") Long senderId);

    /**
     * 마지막 읽은 시간만 업데이트한다 (메시지가 없는 채팅방용).
     * 메시지가 없는 채팅방에서도 lastReadAt을 설정하여,
     * 나중에 메시지가 추가될 때 정확한 unreadCount를 계산할 수 있도록 한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 새로운 읽은 시간
     * @return 업데이트된 행 수
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ChatRoomMemberJpaEntity m SET m.lastReadAt = :lastReadAt " +
           "WHERE m.chatRoomId = :chatRoomId AND m.userId = :userId")
    int updateLastReadAt(@Param("chatRoomId") Long chatRoomId,
                         @Param("userId") Long userId,
                         @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * 여러 메시지의 읽지 않은 멤버 수를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     * 각 메시지 ID별로 읽지 않은 멤버 수를 반환한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param messageIds 메시지 ID 목록
     * @return 메시지 ID와 읽지 않은 멤버 수 배열의 목록 (Object[0]=messageId, Object[1]=unreadCount)
     */
    @Query(value = """
        SELECT m.id as messageId,
               (SELECT COUNT(*) FROM chat_room_members crm
                WHERE crm.chat_room_id = :chatRoomId
                AND crm.user_id != m.sender_id
                AND (crm.last_read_message_id IS NULL OR crm.last_read_message_id < m.id)) as unreadCount
        FROM messages m
        WHERE m.chat_room_id = :chatRoomId AND m.id IN :messageIds
        """, nativeQuery = true)
    List<Object[]> batchCountUnreadMembersByMessageIds(
            @Param("chatRoomId") Long chatRoomId,
            @Param("messageIds") List<Long> messageIds);

    /**
     * 특정 사용자의 여러 채팅방 멤버 정보를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 사용자 ID
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 멤버 목록
     */
    @Query("SELECT m FROM ChatRoomMemberJpaEntity m WHERE m.userId = :userId AND m.chatRoomId IN :chatRoomIds")
    List<ChatRoomMemberJpaEntity> findByUserIdAndChatRoomIdIn(
            @Param("userId") Long userId,
            @Param("chatRoomIds") List<Long> chatRoomIds);

    /**
     * 여러 채팅방의 상대방(본인 제외) 멤버 정보를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     * 1:1 채팅방에서 상대방 정보를 조회할 때 사용한다.
     *
     * @param userId 본인 사용자 ID (제외할 사용자)
     * @param chatRoomIds 채팅방 ID 목록
     * @return 상대방 멤버 목록
     */
    @Query("SELECT m FROM ChatRoomMemberJpaEntity m WHERE m.userId != :userId AND m.chatRoomId IN :chatRoomIds")
    List<ChatRoomMemberJpaEntity> findOtherMembersByUserIdAndChatRoomIdIn(
            @Param("userId") Long userId,
            @Param("chatRoomIds") List<Long> chatRoomIds);
}
