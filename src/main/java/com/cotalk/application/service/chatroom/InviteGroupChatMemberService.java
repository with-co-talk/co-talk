package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidGroupChatException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.chatroom.InviteGroupChatMemberUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 채팅방 멤버 초대 유스케이스 구현체.
 * 그룹 채팅방에 새로운 멤버를 초대한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InviteGroupChatMemberService implements InviteGroupChatMemberUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 그룹 채팅방에 새로운 멤버를 초대한다.
     * 1:1 채팅방에는 멤버를 초대할 수 없으며, 이미 참여 중인 멤버는 건너뛴다.
     *
     * @param roomId 채팅방 ID
     * @param inviterId 초대자 사용자 ID
     * @param inviteeIds 초대할 사용자 ID 목록
     * @throws ChatRoomNotFoundException 채팅방이 존재하지 않는 경우
     * @throws InvalidGroupChatException 1:1 채팅방인 경우
     * @throws ChatRoomAccessDeniedException 초대자가 채팅방 멤버가 아닌 경우
     * @throws UserNotFoundException 초대할 사용자가 존재하지 않는 경우
     */
    @Override
    public void inviteMembers(Long roomId, Long inviterId, List<Long> inviteeIds) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        if (chatRoom.isDirectChat()) {
            throw new InvalidGroupChatException("1:1 채팅방에는 멤버를 초대할 수 없습니다");
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException("채팅방 멤버만 초대할 수 있습니다"));

        for (Long inviteeId : inviteeIds) {
            if (chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviteeId).isPresent()) {
                continue;
            }

            userRepository.findById(inviteeId)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + inviteeId));

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(idGenerator.nextId())
                    .chatRoomId(roomId)
                    .userId(inviteeId)
                    .build();
            chatRoomMemberRepository.save(member);
        }
    }
}
