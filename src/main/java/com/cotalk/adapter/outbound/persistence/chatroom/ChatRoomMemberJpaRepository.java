package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 JPA 리포지토리.
 * Spring Data JPA를 통해 채팅방 멤버 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMember, Long> {

    /**
     * 채팅방 ID와 사용자 ID로 채팅방 멤버를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 멤버 (Optional)
     */
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 채팅방 ID로 모든 멤버 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자 ID로 참여 중인 채팅방 멤버 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByUserId(Long userId);

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
    @Query("UPDATE ChatRoomMember m SET m.lastReadAt = :lastReadAt " +
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
    @Query("UPDATE ChatRoomMember m " +
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
    @Query("SELECT COUNT(m) FROM ChatRoomMember m " +
           "WHERE m.chatRoomId = :chatRoomId " +
           "AND m.userId != :senderId " +
           "AND (m.lastReadAt IS NULL OR m.lastReadAt < :messageCreatedAt)")
    int countUnreadMembers(@Param("chatRoomId") Long chatRoomId,
                           @Param("messageCreatedAt") LocalDateTime messageCreatedAt,
                           @Param("senderId") Long senderId);

    /**
     * 특정 메시지를 읽지 않은 멤버 수를 조회한다. (messageId 기준)
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m " +
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
    @Query("UPDATE ChatRoomMember m SET m.lastReadAt = :lastReadAt " +
           "WHERE m.chatRoomId = :chatRoomId AND m.userId = :userId")
    int updateLastReadAt(@Param("chatRoomId") Long chatRoomId,
                         @Param("userId") Long userId,
                         @Param("lastReadAt") LocalDateTime lastReadAt);
}
