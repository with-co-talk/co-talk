package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅방 멤버 JPA Repository.
 * 
 * <p>Spring Data JPA를 사용한 채팅방 멤버 데이터 저장소 구현입니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Repository
public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMember, UUID> {
    
    /**
     * 채팅방과 사용자로 채팅방 멤버를 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 멤버 엔티티 (없으면 empty)
     */
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    /**
     * 채팅방의 모든 멤버를 조회합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByChatRoomId(UUID chatRoomId);

    /**
     * 사용자가 속한 모든 채팅방 멤버 정보를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByUserId(UUID userId);
}
