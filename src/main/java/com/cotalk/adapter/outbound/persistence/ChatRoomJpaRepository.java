package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {
    
    @Query("SELECT cr FROM ChatRoom cr JOIN ChatRoomMember m ON cr.id = m.chatRoomId WHERE m.userId = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'DIRECT' AND cr.id IN " +
           "(SELECT m1.chatRoomId FROM ChatRoomMember m1 WHERE m1.userId = :userId1) AND cr.id IN " +
           "(SELECT m2.chatRoomId FROM ChatRoomMember m2 WHERE m2.userId = :userId2)")
    Optional<ChatRoom> findDirectChatRoomByUserIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
