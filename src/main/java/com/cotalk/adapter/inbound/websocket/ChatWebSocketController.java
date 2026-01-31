package com.cotalk.adapter.inbound.websocket;

import com.cotalk.adapter.inbound.websocket.dto.AddReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.ChatMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.FileMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresencePingRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresenceInactiveRequest;
import com.cotalk.adapter.inbound.websocket.dto.ReactionBroadcastMessage;
import com.cotalk.adapter.inbound.websocket.dto.RemoveReactionRequest;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase.FileMessageCommand;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.ZoneId;

/**
 * WebSocket 기반 채팅 컨트롤러.
 *
 * <p>클라이언트로부터 WebSocket 메시지를 수신하여 처리하고,
 * Redis Pub/Sub을 통해 모든 서버 인스턴스로 브로드캐스트합니다.</p>
 *
 * <p>지원하는 기능:</p>
 * <ul>
 *     <li>텍스트 메시지 전송</li>
 *     <li>파일 메시지 전송</li>
 *     <li>메시지 반응(이모지) 추가/제거</li>
 * </ul>
 *
 * @author seunggu.lee
 * @see SendMessageUseCase
 * @see AddMessageReactionUseCase
 * @see RemoveMessageReactionUseCase
 * @see ChatMessageBroker
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final ChatMessageBroker chatMessageBroker;
    private final AddMessageReactionUseCase addMessageReactionUseCase;
    private final RemoveMessageReactionUseCase removeMessageReactionUseCase;
    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserEventBroker userEventBroker;
    private final UserRepository userRepository;
    private final ChatRoomPresenceTracker chatRoomPresenceTracker;

    /**
     * 텍스트 채팅 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 텍스트 메시지를 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
     *
     * @param request 채팅 메시지 요청 정보 (발신자 ID, 채팅방 ID, 메시지 내용)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request) {
        log.debug("Received message from user {} to room {}", request.senderId(), request.roomId());

        // 메시지 저장
        Message savedMessage = sendMessageUseCase.sendMessage(
                request.roomId(),
                request.senderId(),
                request.content()
        );

        // Redis Pub/Sub을 통해 모든 서버로 브로드캐스트
        publishToRedis(savedMessage);
    }

    /**
     * 파일 첨부 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 파일 메시지를 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
     *
     * <p>파일 정보에는 URL, 파일명, 크기, 컨텐츠 타입, 썸네일 URL이 포함됩니다.</p>
     *
     * @param request 파일 메시지 요청 정보
     */
    @MessageMapping("/chat/message/file")
    public void sendFileMessage(@Payload FileMessageRequest request) {
        log.debug("Received file message from user {} to room {}", request.senderId(), request.roomId());

        // 파일 메시지 저장
        FileMessageCommand command = new FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        Message savedMessage = sendMessageUseCase.sendFileMessage(
                request.roomId(),
                request.senderId(),
                command
        );

        // Redis Pub/Sub을 통해 모든 서버로 브로드캐스트
        publishToRedis(savedMessage);
    }

    /**
     * 메시지를 Redis Pub/Sub으로 발행
     * Redis Subscriber가 이를 수신하여 WebSocket으로 전달
     *
     * <p>카톡/라인 방식:</p>
     * <ul>
     *   <li>메시지 전송 시 unreadCount = 멤버 수 - 1 (발신자 제외)</li>
     *   <li>상대방이 markAsRead 호출 시 unreadCount 감소</li>
     *   <li>클라이언트가 ReadReceiptEvent를 받아서 UI 업데이트</li>
     * </ul>
     */
    private void publishToRedis(Message message) {
        var members = chatRoomMemberRepository.findByChatRoomId(message.getChatRoomId());
        int totalMembers = members.size();

        // 카톡/라인 방식: 발신자를 제외한 모든 멤버가 읽지 않은 상태로 시작
        // 상대방이 채팅방에 들어가서 markAsRead를 호출하면 unreadCount가 감소함
        int unreadCount = Math.max(0, totalMembers - 1);

        // 발신자 정보 조회
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        String senderNickname = sender != null ? sender.getNickname() : "알 수 없음";
        String senderAvatarUrl = sender != null ? sender.getAvatarUrl() : null;

        log.info(
                "[WS] publishToRedis roomId={}, messageId={}, senderId={}, senderNickname={}, totalMembers={}, unreadCount={}",
                message.getChatRoomId(),
                message.getId(),
                message.getSenderId(),
                senderNickname,
                totalMembers,
                unreadCount
        );

        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                message.getChatRoomId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl(),
                unreadCount,
                null,  // eventType (일반 메시지)
                null,  // relatedUserId
                null   // relatedUserNickname
        );

        chatMessageBroker.publish(message.getChatRoomId(), broadcastMessage);

        // 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트
        publishChatListUpdate(message);
    }

    /**
     * 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트
     */
    private void publishChatListUpdate(Message message) {
        log.debug("Publishing chat list update for message: chatRoomId={}, messageId={}", 
                message.getChatRoomId(), message.getId());

        // 발신자 닉네임 조회
        String senderNickname = userRepository.findById(message.getSenderId())
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 채팅방 참여자 목록 조회
        var members = chatRoomMemberRepository.findByChatRoomId(message.getChatRoomId());
        log.debug("Found {} members in chat room {}", members.size(), message.getChatRoomId());

        for (ChatRoomMember member : members) {
            // 카톡/라인 방식: lastReadMessageId 기준으로 안 읽은 메시지 수 계산
            // 채팅방을 보고 있어도 markAsRead를 호출해야 lastReadMessageId가 업데이트됨
            Long lastReadMessageId = member.getLastReadMessageId();
            int memberUnreadCount = (int) messageRepository.countUnreadMessagesByLastReadMessageId(
                    message.getChatRoomId(),
                    member.getUserId(),
                    lastReadMessageId
            );

            log.info(
                    "[WS] chatListUpdate roomId={}, targetUserId={}, lastReadMessageId={}, memberUnreadCount={}",
                    message.getChatRoomId(),
                    member.getUserId(),
                    lastReadMessageId,
                    memberUnreadCount
            );

            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:" + message.getChatRoomId() + ":" + message.getId() + ":" + member.getUserId(),
                    "NEW_MESSAGE",
                    message.getChatRoomId(),
                    message.getContent(),
                    message.getType().name(),
                    message.getCreatedAt(),
                    message.getSenderId(),
                    senderNickname,
                    memberUnreadCount
            );

            log.debug("Publishing chat list update to user {}: roomId={}, unreadCount={}", 
                    member.getUserId(), message.getChatRoomId(), memberUnreadCount);
            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }

    /**
     * 메시지에 반응(이모지)을 추가합니다.
     *
     * <p>사용자가 특정 메시지에 이모지 반응을 추가하면 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 반응 추가 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 추가 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     */
    @MessageMapping("/chat/reaction/add")
    public void addReaction(@Payload AddReactionRequest request) {
        log.debug("Received reaction add from user {} to message {}", request.userId(), request.messageId());

        MessageReaction reaction = addMessageReactionUseCase.addReaction(
                request.messageId(),
                request.userId(),
                request.emoji()
        );

        // 반응 추가 이벤트를 Redis Pub/Sub으로 브로드캐스트
        publishReactionEvent(reaction, "ADDED");
    }

    /**
     * 메시지에서 반응(이모지)을 제거합니다.
     *
     * <p>사용자가 특정 메시지에서 이모지 반응을 제거하면 데이터베이스에서 삭제하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 반응 제거 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 제거 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     */
    @MessageMapping("/chat/reaction/remove")
    public void removeReaction(@Payload RemoveReactionRequest request) {
        log.debug("Received reaction remove from user {} to message {}", request.userId(), request.messageId());

        removeMessageReactionUseCase.removeReaction(
                request.messageId(),
                request.userId(),
                request.emoji()
        );

        // 반응 제거 이벤트를 Redis Pub/Sub으로 브로드캐스트
        MessageReaction removedReaction = MessageReaction.builder()
                .messageId(request.messageId())
                .userId(request.userId())
                .emoji(Emoji.valueOf(request.emoji()))
                .build();
        publishReactionEvent(removedReaction, "REMOVED");
    }

    /**
     * 채팅방 presence ping.
     * 클라이언트가 방을 보고 있는 동안 주기적으로 호출하여 서버 presence TTL을 갱신한다.
     */
    @MessageMapping("/chat/presence")
    public void presencePing(@Payload PresencePingRequest request, StompHeaderAccessor headerAccessor) {
        if (request == null || request.roomId() == null || request.userId() == null) {
            return;
        }
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        chatRoomPresenceTracker.markActive(request.roomId(), request.userId(), sessionId);
        log.debug("[WS] presencePing roomId={}, userId={}, sessionId={}", request.roomId(), request.userId(), sessionId);
    }

    /**
     * 채팅방 presence inactive.
     * 데스크탑에서 창 포커스가 사라지는 등 "보고 있지 않음" 상태로 전환될 때 호출한다.
     */
    @MessageMapping("/chat/presence/inactive")
    public void presenceInactive(@Payload PresenceInactiveRequest request, StompHeaderAccessor headerAccessor) {
        if (request == null || request.roomId() == null || request.userId() == null) {
            return;
        }
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        chatRoomPresenceTracker.markInactive(request.roomId(), request.userId(), sessionId);
        log.debug("[WS] presenceInactive roomId={}, userId={}, sessionId={}", request.roomId(), request.userId(), sessionId);
    }

    /**
     * 메시지 반응 이벤트를 Redis Pub/Sub으로 발행
     */
    private void publishReactionEvent(MessageReaction reaction, String eventType) {
        // 메시지의 채팅방 ID 조회
        Long chatRoomId = messageRepository.findById(reaction.getMessageId())
                .map(Message::getChatRoomId)
                .orElse(null);

        if (chatRoomId == null) {
            log.warn("Cannot find chat room for message: {}", reaction.getMessageId());
            return;
        }

        ReactionBroadcastMessage broadcastMessage = new ReactionBroadcastMessage(
                1,
                "reaction:" + reaction.getMessageId() + ":" + reaction.getUserId() + ":" + eventType,
                reaction.getId(),
                reaction.getMessageId(),
                reaction.getUserId(),
                reaction.getEmoji().name(), // enum 이름을 문자열로 변환
                eventType,
                reaction.getCreatedAt() != null 
                    ? reaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis()
        );

        // 메시지가 속한 채팅방으로 브로드캐스트
        // 채팅방 ID를 사용하여 브로드캐스트
        chatMessageBroker.publishReaction(chatRoomId, broadcastMessage);
    }
}
