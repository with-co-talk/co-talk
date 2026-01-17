package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.InvalidGroupChatException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

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
        validateUsersExist(creatorId, memberIds);

        ChatRoom chatRoom = ChatRoom.builder()
                .id(idGenerator.nextId())
                .name(roomName)
                .type(ChatRoom.ChatRoomType.GROUP)
                .build();

        chatRoomRepository.save(chatRoom);

        List<Long> allMemberIds = new ArrayList<>();
        allMemberIds.add(creatorId);
        allMemberIds.addAll(memberIds);

        for (Long memberId : allMemberIds) {
            ChatRoomMember member = ChatRoomMember.builder()
                    .id(idGenerator.nextId())
                    .chatRoomId(chatRoom.getId())
                    .userId(memberId)
                    .build();
            chatRoomMemberRepository.save(member);
        }

        return chatRoom.getId();
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

    private void validateUsersExist(Long creatorId, List<Long> memberIds) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + creatorId));

        for (Long memberId : memberIds) {
            userRepository.findById(memberId)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + memberId));
        }
    }
}
