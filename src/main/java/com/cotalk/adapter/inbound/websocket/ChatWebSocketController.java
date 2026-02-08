package com.cotalk.adapter.inbound.websocket;

import com.cotalk.adapter.inbound.websocket.dto.AddReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.ChatMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.FileMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresencePingRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresenceInactiveRequest;
import com.cotalk.adapter.inbound.websocket.dto.ReactionBroadcastMessage;
import com.cotalk.adapter.inbound.websocket.dto.RemoveReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.TypingStatusRequest;
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

import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

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
    private final java.util.Map<Long, String> userNicknameCache = new ConcurrentHashMap<>();

    /**
     * 텍스트 채팅 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 텍스트 메시지를 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
     *
     * @param request 채팅 메시지 요청 정보 (발신자 ID, 채팅방 ID, 메시지 내용)
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);

        if (request == null || request.roomId() == null || request.content() == null || request.content().isBlank()) {
            log.warn("Invalid message request from user {}: {}", authenticatedUserId, request);
            return;
        }

        if (request.content().length() > 5000) {
            log.warn("Message content too long from user {}: {} chars", authenticatedUserId, request.content().length());
            return;
        }

        log.debug("Received message from authenticated user {} to room {}", authenticatedUserId, request.roomId());

        // 메시지 저장 + 컨텍스트 조회 (sender, members 한 번만 조회)
        SendMessageUseCase.SendResult result = sendMessageUseCase.sendMessageWithContext(
                request.roomId(), authenticatedUserId, request.content());

        // Redis Pub/Sub 브로드캐스트 (추가 DB 쿼리 없음)
        publishToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
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
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/message/file")
    public void sendFileMessage(@Payload FileMessageRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);

        if (request == null || request.roomId() == null || request.fileUrl() == null || request.fileUrl().isBlank()) {
            log.warn("Invalid file message request from user {}: {}", authenticatedUserId, request);
            return;
        }

        log.debug("Received file message from authenticated user {} to room {}", authenticatedUserId, request.roomId());

        FileMessageCommand command = new FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        // 메시지 저장 + 컨텍스트 조회 (sender, members 한 번만 조회)
        SendMessageUseCase.SendResult result = sendMessageUseCase.sendFileMessageWithContext(
                request.roomId(), authenticatedUserId, command);

        // Redis Pub/Sub 브로드캐스트 (추가 DB 쿼리 없음)
        publishToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
    }

    /**
     * StompHeaderAccessor에서 인증된 사용자 ID를 추출합니다.
     *
     * @param headerAccessor WebSocket 헤더 접근자
     * @return 인증된 사용자 ID
     * @throws IllegalArgumentException 사용자 인증 정보가 없는 경우
     */
    private Long extractUserId(StompHeaderAccessor headerAccessor) {
        if (headerAccessor == null || headerAccessor.getUser() == null) {
            throw new IllegalArgumentException("User authentication information is missing");
        }
        String userIdStr = headerAccessor.getUser().getName();
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userIdStr, e);
        }
    }

    /**
     * 메시지를 Redis Pub/Sub으로 발행한다.
     * 사전 조회된 sender와 members 정보를 사용하여 추가 DB 쿼리를 방지한다.
     *
     * <p>카톡/라인 방식:</p>
     * <ul>
     *   <li>메시지 전송 시 unreadCount = 멤버 수 - 1 (발신자 제외)</li>
     *   <li>상대방이 markAsRead 호출 시 unreadCount 감소</li>
     *   <li>클라이언트가 ReadReceiptEvent를 받아서 UI 업데이트</li>
     * </ul>
     *
     * @param message 발행할 메시지
     * @param senderNickname 발신자 닉네임 (사전 조회됨)
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (사전 조회됨)
     * @param members 채팅방 멤버 목록 (사전 조회됨)
     */
    private void publishToRedis(Message message, String senderNickname, String senderAvatarUrl,
                                 java.util.List<ChatRoomMember> members) {
        // 카톡/라인 방식: 발신자를 제외한 모든 멤버가 읽지 않은 상태로 시작
        int unreadCount = Math.max(0, members.size() - 1);

        log.debug(
                "[WS] publishToRedis roomId={}, messageId={}, senderId={}, senderNickname={}, totalMembers={}, unreadCount={}",
                message.getChatRoomId(), message.getId(), message.getSenderId(),
                senderNickname, members.size(), unreadCount);

        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                message.getChatRoomId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
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
        publishChatListUpdate(message, members, senderNickname);
    }

    /**
     * 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트
     *
     * <p>성능 최적화: 배치 쿼리를 사용하여 모든 멤버의 unreadCount를 한 번에 조회한다.
     * (기존 N+1 쿼리 문제 해결)</p>
     *
     * @param message 전송된 메시지
     * @param members 채팅방 멤버 목록 (중복 쿼리 방지용)
     * @param senderNickname 발신자 닉네임 (중복 쿼리 방지용)
     */
    private void publishChatListUpdate(Message message, java.util.List<ChatRoomMember> members, String senderNickname) {
        log.debug("Publishing chat list update for message: chatRoomId={}, messageId={}",
                message.getChatRoomId(), message.getId());

        log.debug("Found {} members in chat room {}", members.size(), message.getChatRoomId());

        // 배치 쿼리로 모든 멤버의 unreadCount를 한 번에 조회 (N+1 쿼리 방지)
        java.util.Map<Long, Long> unreadCountMap = messageRepository.batchCountUnreadMessagesForAllMembers(
                message.getChatRoomId());

        for (ChatRoomMember member : members) {
            int memberUnreadCount = unreadCountMap.getOrDefault(member.getUserId(), 0L).intValue();

            log.debug(
                    "[WS] chatListUpdate roomId={}, targetUserId={}, lastReadMessageId={}, memberUnreadCount={}",
                    message.getChatRoomId(),
                    member.getUserId(),
                    member.getLastReadMessageId(),
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
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/reaction/add")
    public void addReaction(@Payload AddReactionRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);
        log.debug("Received reaction add from authenticated user {} to message {}", authenticatedUserId, request.messageId());

        MessageReaction reaction = addMessageReactionUseCase.addReaction(
                request.messageId(),
                authenticatedUserId,
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
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/reaction/remove")
    public void removeReaction(@Payload RemoveReactionRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);
        log.debug("Received reaction remove from authenticated user {} to message {}", authenticatedUserId, request.messageId());

        removeMessageReactionUseCase.removeReaction(
                request.messageId(),
                authenticatedUserId,
                request.emoji()
        );

        // 반응 제거 이벤트를 Redis Pub/Sub으로 브로드캐스트
        MessageReaction removedReaction = MessageReaction.builder()
                .messageId(request.messageId())
                .userId(authenticatedUserId)
                .emoji(Emoji.fromString(request.emoji())
                        .orElseThrow(() -> new com.cotalk.domain.exception.InvalidEmojiException(request.emoji())))
                .build();
        publishReactionEvent(removedReaction, "REMOVED");
    }

    /**
     * 타이핑 상태를 브로드캐스트한다.
     * 클라이언트가 타이핑 시작/중지를 알리면 같은 채팅방의 다른 멤버들에게 전달한다.
     *
     * @param request        타이핑 상태 요청 (채팅방 ID, 타이핑 여부)
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/typing")
    public void typingStatus(@Payload TypingStatusRequest request, StompHeaderAccessor headerAccessor) {
        if (request == null || request.roomId() == null) {
            return;
        }
        Long authenticatedUserId = extractUserId(headerAccessor);
        String userNickname = userNicknameCache.computeIfAbsent(authenticatedUserId,
                id -> userRepository.findById(id).map(User::getNickname).orElse(null));

        String eventType = Boolean.TRUE.equals(request.isTyping()) ? "TYPING" : "STOP_TYPING";
        chatMessageBroker.publishRoomEvent(request.roomId(), new TypingBroadcastEvent(
                1,
                "typing:" + request.roomId() + ":" + authenticatedUserId + ":" + System.currentTimeMillis(),
                eventType,
                request.roomId(),
                authenticatedUserId,
                userNickname,
                request.isTyping()
        ));
        log.debug("[WS] typingStatus roomId={}, userId={}, isTyping={}", request.roomId(), authenticatedUserId, request.isTyping());
    }

    /**
     * 타이핑 브로드캐스트 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달된다.
     */
    private record TypingBroadcastEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            String userNickname,
            Boolean isTyping
    ) {}

    /**
     * 채팅방 presence ping.
     * 클라이언트가 방을 보고 있는 동안 주기적으로 호출하여 서버 presence TTL을 갱신한다.
     */
    @MessageMapping("/chat/presence")
    public void presencePing(@Payload PresencePingRequest request, StompHeaderAccessor headerAccessor) {
        if (request == null || request.roomId() == null) {
            return;
        }
        Long authenticatedUserId = extractUserId(headerAccessor);
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        chatRoomPresenceTracker.markActive(request.roomId(), authenticatedUserId, sessionId);
        log.debug("[WS] presencePing roomId={}, userId={}, sessionId={}", request.roomId(), authenticatedUserId, sessionId);
    }

    /**
     * 채팅방 presence inactive.
     * 데스크탑에서 창 포커스가 사라지는 등 "보고 있지 않음" 상태로 전환될 때 호출한다.
     */
    @MessageMapping("/chat/presence/inactive")
    public void presenceInactive(@Payload PresenceInactiveRequest request, StompHeaderAccessor headerAccessor) {
        if (request == null || request.roomId() == null) {
            return;
        }
        Long authenticatedUserId = extractUserId(headerAccessor);
        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        chatRoomPresenceTracker.markInactive(request.roomId(), authenticatedUserId, sessionId);
        log.debug("[WS] presenceInactive roomId={}, userId={}, sessionId={}", request.roomId(), authenticatedUserId, sessionId);
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
                reaction.getEmoji().getCharacter(), // 유니코드 이모지 문자 전송
                eventType,
                reaction.getCreatedAt() != null 
                    ? reaction.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                    : System.currentTimeMillis()
        );

        // 메시지가 속한 채팅방으로 브로드캐스트
        // 채팅방 ID를 사용하여 브로드캐스트
        chatMessageBroker.publishReaction(chatRoomId, broadcastMessage);
    }
}
