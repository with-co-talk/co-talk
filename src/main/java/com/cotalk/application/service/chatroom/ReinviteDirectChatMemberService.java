package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.chatroom.ReinviteDirectChatMemberUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.BlockValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 1:1 채팅방 멤버 재초대 유스케이스 구현체.
 * 1:1 채팅방에서 나간 상대방을 다시 초대한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReinviteDirectChatMemberService implements ReinviteDirectChatMemberUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final MessageRepository messageRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;
    private final BlockValidator blockValidator;

    /**
     * 1:1 채팅방에서 나간 상대방을 재초대한다.
     * 재초대 시 시스템 메시지가 생성되고, 모든 멤버에게 브로드캐스트된다.
     *
     * @param roomId 채팅방 ID
     * @param inviterId 초대하는 사용자 ID (현재 채팅방에 남아있는 사용자)
     * @param inviteeId 재초대할 사용자 ID (나갔던 사용자)
     * @throws ChatRoomNotFoundException 채팅방이 존재하지 않는 경우
     * @throws InvalidChatRoomException 1:1 채팅방이 아닌 경우
     * @throws ChatRoomAccessDeniedException 초대자가 채팅방 멤버가 아닌 경우
     * @throws UserNotFoundException 재초대할 사용자가 존재하지 않는 경우
     * @throws InvalidChatRoomException 재초대할 사용자가 이미 채팅방 멤버인 경우
     */
    @Override
    public void reinviteMember(Long roomId, Long inviterId, Long inviteeId) {
        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));

        // 2. 1:1 채팅방인지 확인
        if (!chatRoom.isDirectChat()) {
            throw InvalidChatRoomException.invalidDirectChat("1:1 채팅방에서만 재초대할 수 있습니다");
        }

        // 3. 초대자가 채팅방 멤버인지 확인
        chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException("채팅방 멤버만 재초대할 수 있습니다"));

        // 4. 재초대할 사용자가 이미 채팅방 멤버인지 확인
        if (chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviteeId).isPresent()) {
            throw new InvalidChatRoomException("이미 채팅방 멤버입니다");
        }

        // 5. 재초대할 사용자 존재 확인
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new UserNotFoundException(inviteeId));

        // 5-1. 차단 관계 검증 (양방향): 초대자-피초대자 사이에 차단이 있으면 재초대 거부
        blockValidator.validateNotBlocked(inviterId, inviteeId);

        // 6. 새 멤버 추가
        ChatRoomMember newMember = ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(inviteeId)
                .build();
        chatRoomMemberRepository.save(newMember);
        log.info("User reinvited to direct chat room: userId={}, chatRoomId={}", inviteeId, roomId);

        // 7. 시스템 메시지 생성 및 브로드캐스트
        sendReinviteSystemMessage(roomId, inviteeId, invitee.getNickname());
    }

    /**
     * 재초대 시스템 메시지를 생성하고 브로드캐스트한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param inviteeId 재초대된 사용자 ID
     * @param inviteeNickname 재초대된 사용자 닉네임
     */
    private void sendReinviteSystemMessage(Long chatRoomId, Long inviteeId, String inviteeNickname) {
        String systemMessageContent = inviteeNickname + "님이 다시 참여했습니다";

        // 시스템 메시지 생성
        Message systemMessage = Message.createSystemMessage(
                idGenerator.nextId(),
                chatRoomId,
                systemMessageContent
        );

        // 메시지 저장
        Message savedMessage = messageRepository.save(systemMessage);
        log.info("System message created for user reinvite: chatRoomId={}, messageId={}",
                chatRoomId, savedMessage.getId());

        // 현재 멤버 목록 조회 (재초대된 사용자 포함)
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        // WebSocket으로 브로드캐스트
        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                savedMessage.getId(),
                savedMessage.getSenderId(),
                null, // senderNickname (시스템 메시지)
                null, // senderAvatarUrl (시스템 메시지)
                savedMessage.getChatRoomId(),
                savedMessage.getContent(),
                savedMessage.getType().name(),
                savedMessage.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                null, // fileUrl
                null, // fileName
                null, // fileSize
                null, // fileContentType
                null, // thumbnailUrl
                0,    // unreadCount (시스템 메시지는 읽음 처리 불필요)
                "USER_JOINED",   // eventType
                inviteeId,       // relatedUserId
                inviteeNickname, // relatedUserNickname
                null             // clientMessageId (시스템 메시지, 해당 경로 없음)
        );

        chatMessageBroker.publish(chatRoomId, broadcastMessage);

        // 채팅 목록 업데이트 이벤트 전송
        for (ChatRoomMember member : members) {
            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:" + chatRoomId + ":" + savedMessage.getId() + ":" + member.getUserId(),
                    "USER_REINVITED",
                    chatRoomId,
                    systemMessageContent,
                    savedMessage.getType().name(),
                    savedMessage.getCreatedAt(),
                    0L,
                    inviteeNickname,
                    0 // unreadCount
            );
            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }
}
