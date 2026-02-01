package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 JPA 리포지토리.
 * Spring Data JPA를 통해 채팅방 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 사용자 ID로 참여 중인 채팅방 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN ChatRoomMember m ON cr.id = m.chatRoomId WHERE m.userId = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    /**
     * 두 사용자 간의 1:1 채팅방을 조회한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 1:1 채팅방 (Optional)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'DIRECT' AND cr.id IN " +
           "(SELECT m1.chatRoomId FROM ChatRoomMember m1 WHERE m1.userId = :userId1) AND cr.id IN " +
           "(SELECT m2.chatRoomId FROM ChatRoomMember m2 WHERE m2.userId = :userId2)")
    Optional<ChatRoom> findDirectChatRoomByUserIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 사용자의 나와의 채팅방(SELF)을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 나와의 채팅방 (Optional)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'SELF' AND cr.id IN " +
           "(SELECT m.chatRoomId FROM ChatRoomMember m WHERE m.userId = :userId)")
    Optional<ChatRoom> findSelfChatRoomByUserId(@Param("userId") Long userId);
}
