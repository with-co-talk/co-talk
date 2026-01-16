package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.CreateChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Long createChatRoom(Long userId1, Long userId2) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findDirectChatRoomByUserIds(userId1, userId2);
        if (existingRoom.isPresent()) {
            return existingRoom.get().getId();
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .id(idGenerator.nextId())
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        chatRoomRepository.save(chatRoom);

        ChatRoomMember member1 = ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoom.getId())
                .userId(userId1)
                .build();

        ChatRoomMember member2 = ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoom.getId())
                .userId(userId2)
                .build();

        chatRoomMemberRepository.save(member1);
        chatRoomMemberRepository.save(member2);

        return chatRoom.getId();
    }
}
