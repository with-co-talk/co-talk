package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅방 멤버 Repository 어댑터.
 * 
 * <p>도메인 Repository 인터페이스를 JPA Repository로 구현합니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryAdapter implements ChatRoomMemberRepository {

    private final ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        return chatRoomMemberJpaRepository.save(member);
    }

    @Override
    public Optional<ChatRoomMember> findById(UUID id) {
        return chatRoomMemberJpaRepository.findById(id);
    }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId) {
        return chatRoomMemberJpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Override
    public List<ChatRoomMember> findByChatRoomId(UUID chatRoomId) {
        return chatRoomMemberJpaRepository.findByChatRoomId(chatRoomId);
    }

    @Override
    public List<ChatRoomMember> findByUserId(UUID userId) {
        return chatRoomMemberJpaRepository.findByUserId(userId);
    }

    @Override
    public void delete(ChatRoomMember member) {
        chatRoomMemberJpaRepository.delete(member);
    }
}
