package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 1:1 채팅방 생성 유스케이스 구현체.
 * 두 사용자 간의 1:1 채팅방을 생성한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final IdGenerator idGenerator;
    private final UserEventBroker userEventBroker;

    /**
     * 1:1 채팅방을 생성한다.
     * 이미 두 사용자 간의 채팅방이 존재하면 기존 채팅방 ID를 반환한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 생성된 또는 기존 채팅방 ID
     */
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

        publishRoomCreatedEvent(chatRoom.getId(), userId1);

        return chatRoom.getId();
    }

    private void publishRoomCreatedEvent(Long chatRoomId, Long userId1) {
        ChatListUpdateEvent event = new ChatListUpdateEvent(
                1,
                "chat-list:ROOM_CREATED:" + chatRoomId + ":" + userId1,
                "ROOM_CREATED",
                chatRoomId,
                "",
                "SYSTEM",
                LocalDateTime.now(),
                userId1,
                "SYSTEM",
                0
        );
        // 1:1 채팅은 "대화하기"로 방만 만든 단계에서는 상대방 목록에 노출되지 않아야 한다.
        // 따라서 ROOM_CREATED 이벤트는 요청자(보통 userId1)에게만 보낸다.
        userEventBroker.publishChatListUpdate(userId1, event);
    }
}
