package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
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
import com.cotalk.domain.port.outbound.DistributedLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 채팅방 나가기 유스케이스 구현체.
 * 사용자가 채팅방에서 나간다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>분산락: 동일 채팅방에 대한 동시 퇴장 방지</li>
 * </ul>
 *
 * <p><b>방/메시지 이력 보존 정책</b>: 멤버가 나가도 채팅방과 메시지 이력은
 * 삭제하지 않고 보존한다. 이는 의도된 도메인 규칙으로, 잔존 멤버 수에 따라
 * 보존의 직접적인 근거가 다르다.
 * <ul>
 *   <li><b>1명 잔존(1:1에서 상대만 나감)</b>: 남은 멤버가
 *       {@code ReinviteDirectChatMemberService.reinviteMember}로 같은 방에 다시
 *       초대할 수 있다(재초대는 초대자가 여전히 그 방의 멤버여야 동작한다).
 *       이때 기존 메시지 이력이 그대로 유지되어 대화가 이어진다.</li>
 *   <li><b>0명(마지막 멤버까지 나감)</b>: 잔존 멤버가 없어 재초대로 되살릴 수 없으므로
 *       보존의 직접 근거는 이력/감사(대화 기록 보존)이다. 재초대 재사용은 이 분기에
 *       해당하지 않는다.</li>
 * </ul>
 *
 * <p><b>운영 영향</b>: 멤버가 0명인 빈 방이 누적될 수 있다(특히 그룹 채팅에서
 * 재초대 경로가 없을 때). 보관 비용이 문제가 되면 배치성 정리 작업(예: 일정 기간
 * 멤버 0명 + 신규 메시지 없음인 방을 연관 데이터와 함께 삭제)을 별도로 도입한다.
 * 퇴장 경로에서 동기 삭제하지 않는 이유는 위 이력/감사 및 (1명 잔존 시) 재초대
 * 재사용 규칙 때문이다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveChatRoomService implements LeaveChatRoomUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final DistributedLockPort lockExecutor;
    private final IdGenerator idGenerator;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;
    private final TransactionTemplate transactionTemplate;

    /**
     * 채팅방에서 나간다.
     * 마지막 멤버가 나가도 채팅방과 메시지 이력은 보존된다(0명은 이력/감사,
     * 1명 잔존 시 재초대 재사용 목적). 자세한 보존 정책은 클래스 JavaDoc 참고.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>채팅방 단위 분산락으로 동시 퇴장 방지</li>
     *   <li>멤버 삭제와 시스템 메시지 처리가 단일 트랜잭션 내에서 실행</li>
     * </ul>
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     나가는 사용자 ID
     * @throws ChatRoomNotFoundException     채팅방이 존재하지 않는 경우
     * @throws InvalidChatRoomException      나와의 채팅방(SELF)에서 나가려는 경우
     * @throws ChatRoomAccessDeniedException 해당 채팅방의 멤버가 아닌 경우
     */
    @Override
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        String lockKey = "chatroom:leave:" + chatRoomId;

        lockExecutor.executeWithLock(lockKey, () ->
            transactionTemplate.executeWithoutResult(status -> doLeaveChatRoom(chatRoomId, userId))
        );
    }

    /**
     * 채팅방 퇴장을 실행한다.
     * 분산락 내부에서 실행되어 동시성이 보장된다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     */
    private void doLeaveChatRoom(Long chatRoomId, Long userId) {
        // SELF 채팅방 퇴장 방지
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));
        if (chatRoom.isSelfChat()) {
            throw new InvalidChatRoomException("나와의 채팅방에서는 나갈 수 없습니다.");
        }

        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(chatRoomId, userId));

        // 사용자 닉네임 조회
        String userNickname = userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 멤버 삭제
        chatRoomMemberRepository.delete(member);
        log.info("User left chat room: userId={}, chatRoomId={}", userId, chatRoomId);

        // 남은 멤버 확인
        List<ChatRoomMember> remainingMembers = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        if (remainingMembers.isEmpty()) {
            // 빈 방 보존 정책: 마지막 멤버가 나가도 방/메시지 이력은 삭제하지 않는다.
            // 0명 방은 잔존 멤버가 없어 재초대로 되살릴 수 없으므로 보존 근거는 이력/감사.
            // (재초대 재사용은 1명 잔존 케이스에 해당 — 클래스 JavaDoc 참고)
            // (운영상 빈 방 누적은 별도 배치 정리로 처리 — 클래스 JavaDoc 참고)
            log.info("Chat room has no remaining members; retained for history/audit: chatRoomId={}", chatRoomId);
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
                savedMessage.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
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
