package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.InvalidGroupChatException;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 그룹 채팅방 생성 유스케이스 구현체.
 * 3명 이상의 사용자가 참여하는 그룹 채팅방을 생성한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateGroupChatRoomService implements CreateGroupChatRoomUseCase {

    private static final int MIN_GROUP_MEMBERS = 3;
    private static final int MAX_ROOM_NAME_LENGTH = 50;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserValidator userValidator;
    private final IdGenerator idGenerator;
    private final UserEventBroker userEventBroker;

    /**
     * 그룹 채팅방을 생성한다.
     * 최소 3명 이상의 멤버가 필요하며, 채팅방 이름은 50자 이내여야 한다.
     *
     * @param creatorId 생성자 사용자 ID
     * @param roomName 채팅방 이름
     * @param memberIds 초대할 멤버 ID 목록 (생성자 제외)
     * @return 생성된 채팅방 ID
     * @throws InvalidGroupChatException 채팅방 이름이 유효하지 않거나 멤버 수가 부족한 경우
     * @throws UserNotFoundException 존재하지 않는 사용자가 포함된 경우
     */
    @Override
    public Long createGroupChatRoom(Long creatorId, String roomName, List<Long> memberIds) {
        validateRoomName(roomName);
        validateMemberCount(memberIds);

        List<Long> allUserIds = Stream.concat(Stream.of(creatorId), memberIds.stream()).toList();
        userValidator.validateUsersExist(allUserIds);

        ChatRoom chatRoom = ChatRoom.builder()
                .id(idGenerator.nextId())
                .name(roomName)
                .type(ChatRoom.ChatRoomType.GROUP)
                .build();

        chatRoomRepository.save(chatRoom);

        List<Long> allMemberIds = new ArrayList<>();
        allMemberIds.add(creatorId);
        allMemberIds.addAll(memberIds);

        List<ChatRoomMember> members = allMemberIds.stream()
                .map(memberId -> ChatRoomMember.builder()
                        .id(idGenerator.nextId())
                        .chatRoomId(chatRoom.getId())
                        .userId(memberId)
                        .build())
                .toList();
        chatRoomMemberRepository.saveAll(members);

        publishRoomCreatedEvent(chatRoom.getId(), creatorId, allMemberIds);

        return chatRoom.getId();
    }

    private void publishRoomCreatedEvent(Long chatRoomId, Long creatorId, List<Long> allMemberIds) {
        ChatListUpdateEvent event = new ChatListUpdateEvent(
                1,
                "chat-list:ROOM_CREATED:" + chatRoomId + ":" + creatorId,
                "ROOM_CREATED",
                chatRoomId,
                "",
                "SYSTEM",
                LocalDateTime.now(),
                creatorId,
                "SYSTEM",
                0
        );
        for (Long memberId : allMemberIds) {
            userEventBroker.publishChatListUpdate(memberId, event);
        }
    }

    private void validateRoomName(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            throw new InvalidGroupChatException("그룹 채팅방 이름은 필수입니다");
        }
        if (roomName.length() > MAX_ROOM_NAME_LENGTH) {
            throw new InvalidGroupChatException("그룹 채팅방 이름은 50자를 초과할 수 없습니다");
        }
    }

    private void validateMemberCount(List<Long> memberIds) {
        // 생성자 + memberIds = 최소 3명
        if (memberIds == null || memberIds.size() < MIN_GROUP_MEMBERS - 1) {
            throw new InvalidGroupChatException("그룹 채팅방은 최소 3명 이상이어야 합니다");
        }
    }
}
