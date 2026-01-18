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
    @Modifying
    @Query("UPDATE ChatRoomMember m SET m.lastReadAt = :lastReadAt " +
           "WHERE m.chatRoomId = :chatRoomId AND m.userId = :userId " +
           "AND (m.lastReadAt IS NULL OR m.lastReadAt < :lastReadAt)")
    int updateLastReadAtIfNewer(@Param("chatRoomId") Long chatRoomId,
                                @Param("userId") Long userId,
                                @Param("lastReadAt") LocalDateTime lastReadAt);
}
