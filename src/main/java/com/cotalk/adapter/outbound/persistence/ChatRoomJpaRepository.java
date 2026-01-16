package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅방 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 채팅방 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, UUID> {
    
    /**
     * 사용자가 속한 모든 채팅방을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 목록
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN ChatRoomMember crm ON cr.id = crm.chatRoomId " +
           "WHERE crm.userId = :userId " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") UUID userId);

    /**
     * 두 사용자 간의 1:1 채팅방을 조회합니다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 채팅방 엔티티 (없으면 empty)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.type = 'DIRECT' " +
           "AND EXISTS (SELECT 1 FROM ChatRoomMember crm1 WHERE crm1.chatRoomId = cr.id AND crm1.userId = :userId1) " +
           "AND EXISTS (SELECT 1 FROM ChatRoomMember crm2 WHERE crm2.chatRoomId = cr.id AND crm2.userId = :userId2) " +
           "AND (SELECT COUNT(crm) FROM ChatRoomMember crm WHERE crm.chatRoomId = cr.id) = 2")
    Optional<ChatRoom> findDirectChatRoomByUserIds(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
