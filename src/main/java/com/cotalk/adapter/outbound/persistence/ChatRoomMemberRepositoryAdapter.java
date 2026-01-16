package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryAdapter implements ChatRoomMemberRepository {

    private final ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        return chatRoomMemberJpaRepository.save(member);
    }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return chatRoomMemberJpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Override
    public List<ChatRoomMember> findByChatRoomId(Long chatRoomId) {
        return chatRoomMemberJpaRepository.findByChatRoomId(chatRoomId);
    }

    @Override
    public List<ChatRoomMember> findByUserId(Long userId) {
        return chatRoomMemberJpaRepository.findByUserId(userId);
    }

    @Override
    public void delete(ChatRoomMember member) {
        chatRoomMemberJpaRepository.delete(member);
    }
}
