package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomJpaRepository.save(chatRoom);
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomJpaRepository.findById(id);
    }

    @Override
    public List<ChatRoom> findByUserId(Long userId) {
        return chatRoomJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<ChatRoom> findDirectChatRoomByUserIds(Long userId1, Long userId2) {
        return chatRoomJpaRepository.findDirectChatRoomByUserIds(userId1, userId2);
    }
}
