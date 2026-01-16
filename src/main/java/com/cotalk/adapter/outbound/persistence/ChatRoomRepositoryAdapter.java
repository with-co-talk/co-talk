package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.port.outbound.persistence.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅방 Repository 어댑터.
 * 
 * <p>도메인 Repository 인터페이스를 JPA Repository로 구현합니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomJpaRepository.save(chatRoom);
    }

    @Override
    public Optional<ChatRoom> findById(UUID id) {
        return chatRoomJpaRepository.findById(id);
    }

    @Override
    public List<ChatRoom> findByUserId(UUID userId) {
        return chatRoomJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<ChatRoom> findDirectChatRoomByUserIds(UUID userId1, UUID userId2) {
        return chatRoomJpaRepository.findDirectChatRoomByUserIds(userId1, userId2);
    }
}
