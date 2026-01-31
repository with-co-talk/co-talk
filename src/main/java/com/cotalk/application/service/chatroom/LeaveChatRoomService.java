package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.chatroom.LeaveChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

/**
 * 채팅방 나가기 유스케이스 구현체.
 * 사용자가 채팅방에서 나간다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>분산락: 동일 채팅방에 대한 동시 퇴장 방지</li>
 *   <li>마지막 멤버 퇴장 시 채팅방 삭제가 원자적으로 처리됨</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveChatRoomService implements LeaveChatRoomUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final DistributedLockExecutor lockExecutor;
    private final IdGenerator idGenerator;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;

    /**
     * 채팅방에서 나간다.
     * 마지막 멤버가 나가면 채팅방도 함께 삭제된다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>채팅방 단위 분산락으로 동시 퇴장 방지</li>
     *   <li>멤버 삭제와 채팅방 삭제가 원자적으로 실행</li>
     * </ul>
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     나가는 사용자 ID
     * @throws ChatRoomAccessDeniedException 해당 채팅방의 멤버가 아닌 경우
     */
    @Override
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        String lockKey = "chatroom:leave:" + chatRoomId;

        lockExecutor.executeWithLock(lockKey, () -> doLeaveChatRoom(chatRoomId, userId));
    }

    /**
     * 채팅방 퇴장을 실행한다.
     * 분산락 내부에서 실행되어 동시성이 보장된다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     */
    @Transactional
    protected void doLeaveChatRoom(Long chatRoomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(chatRoomId, userId));

        // 사용자 닉네임 조회
        String userNickname = userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 멤버 삭제
        chatRoomMemberRepository.delete(member);
        log.info("User left chat room: userId={}, chatRoomId={}", userId, chatRoomId);

        // 남은 멤버 확인 및 채팅방 삭제 (원자적 처리)
        List<ChatRoomMember> remainingMembers = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        if (remainingMembers.isEmpty()) {
            chatRoomRepository.findById(chatRoomId).ifPresent(chatRoom -> {
                chatRoomRepository.delete(chatRoom);
                log.info("Chat room deleted as last member left: chatRoomId={}", chatRoomId);
            });
        } else {
            // 시스템 메시지 생성 및 브로드캐스트 (남은 멤버가 있을 때만)
            sendLeaveSystemMessage(chatRoomId, userId, userNickname, remainingMembers);
        }
    }

    /**
     * 채팅방 퇴장 시스템 메시지를 생성하고 브로드캐스트한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param leavingUserId 나가는 사용자 ID
     * @param userNickname 나가는 사용자 닉네임
     * @param remainingMembers 남은 멤버 목록
     */
    private void sendLeaveSystemMessage(Long chatRoomId, Long leavingUserId, String userNickname,
                                        List<ChatRoomMember> remainingMembers) {
        String systemMessageContent = userNickname + "님이 나갔습니다";

        // 시스템 메시지 생성
        Message systemMessage = Message.createSystemMessage(
                idGenerator.nextId(),
                chatRoomId,
                systemMessageContent
        );

        // 메시지 저장
        Message savedMessage = messageRepository.save(systemMessage);
        log.info("System message created for user leave: chatRoomId={}, messageId={}, content={}",
                chatRoomId, savedMessage.getId(), systemMessageContent);

        // WebSocket으로 브로드캐스트
        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                savedMessage.getId(),
                savedMessage.getSenderId(),
                null, // senderNickname (시스템 메시지)
                null, // senderAvatarUrl (시스템 메시지)
                savedMessage.getChatRoomId(),
                savedMessage.getContent(),
                savedMessage.getType().name(),
                savedMessage.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                null, // fileUrl
                null, // fileName
                null, // fileSize
                null, // fileContentType
                null, // thumbnailUrl
                0,    // unreadCount (시스템 메시지는 읽음 처리 불필요)
                "USER_LEFT",     // eventType
                leavingUserId,   // relatedUserId
                userNickname     // relatedUserNickname
        );

        chatMessageBroker.publish(chatRoomId, broadcastMessage);

        // 채팅 목록 업데이트 이벤트 전송
        for (ChatRoomMember member : remainingMembers) {
            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:" + chatRoomId + ":" + savedMessage.getId() + ":" + member.getUserId(),
                    "USER_LEFT",
                    chatRoomId,
                    systemMessageContent,
                    savedMessage.getType().name(),
                    savedMessage.getCreatedAt(),
                    leavingUserId,
                    userNickname,
                    0 // unreadCount
            );
            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }
}
