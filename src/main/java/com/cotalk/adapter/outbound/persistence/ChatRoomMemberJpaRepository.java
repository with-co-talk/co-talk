package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberJpaRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);
    List<ChatRoomMember> findByUserId(Long userId);
}
