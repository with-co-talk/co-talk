package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.chat.CreateChatRoomUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.persistence.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 채팅방 생성 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public UUID createChatRoom(UUID userId1, UUID userId2) {
        // 이미 존재하는 채팅방 확인
        return chatRoomRepository.findDirectChatRoomByUserIds(userId1, userId2)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    // 새 채팅방 생성
                    ChatRoom chatRoom = ChatRoom.builder()
                            .type(ChatRoom.ChatRoomType.DIRECT)
                            .build();
                    ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

                    // 채팅방 멤버 추가
                    ChatRoomMember member1 = ChatRoomMember.builder()
                            .chatRoomId(savedChatRoom.getId())
                            .userId(userId1)
                            .build();

                    ChatRoomMember member2 = ChatRoomMember.builder()
                            .chatRoomId(savedChatRoom.getId())
                            .userId(userId2)
                            .build();

                    chatRoomMemberRepository.save(member1);
                    chatRoomMemberRepository.save(member2);

                    return savedChatRoom.getId();
                });
    }
}
