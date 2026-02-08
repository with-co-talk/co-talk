package com.cotalk.adapter.inbound.websocket;

import com.cotalk.adapter.inbound.websocket.dto.AddReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.ChatMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.FileMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresencePingRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresenceInactiveRequest;
import com.cotalk.adapter.inbound.websocket.dto.RemoveReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.TypingStatusRequest;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.port.inbound.chat.BroadcastChatMessageUseCase;
import com.cotalk.domain.port.inbound.chat.BroadcastReactionEventUseCase;
import com.cotalk.domain.port.inbound.chat.PublishChatListUpdateUseCase;
import com.cotalk.domain.port.inbound.chat.PublishTypingStatusUseCase;
import com.cotalk.domain.port.inbound.chat.UpdatePresenceStatusUseCase;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase.ReactionResult;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase.FileMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 기반 채팅 컨트롤러.
 *
 * <p>클라이언트로부터 WebSocket 메시지를 수신하여 처리하고,
 * 인바운드 유스케이스 포트를 통해 비즈니스 로직을 실행합니다.</p>
 *
 * <p>헥사고날 아키텍처를 준수하여 아웃바운드 포트에 직접 의존하지 않으며,
 * 모든 외부 시스템 접근은 인바운드 유스케이스를 통해 이루어집니다.</p>
 *
 * <p>지원하는 기능:</p>
 * <ul>
 *     <li>텍스트 메시지 전송</li>
 *     <li>파일 메시지 전송</li>
 *     <li>메시지 반응(이모지) 추가/제거</li>
 *     <li>타이핑 상태 브로드캐스트</li>
 *     <li>채팅방 presence 상태 관리</li>
 * </ul>
 *
 * @author seunggu.lee
 * @see SendMessageUseCase
 * @see AddMessageReactionUseCase
 * @see RemoveMessageReactionUseCase
 * @see BroadcastChatMessageUseCase
 * @see BroadcastReactionEventUseCase
 * @see PublishTypingStatusUseCase
 * @see UpdatePresenceStatusUseCase
 * @see PublishChatListUpdateUseCase
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final AddMessageReactionUseCase addMessageReactionUseCase;
    private final RemoveMessageReactionUseCase removeMessageReactionUseCase;
    private final BroadcastChatMessageUseCase broadcastChatMessageUseCase;
    private final BroadcastReactionEventUseCase broadcastReactionEventUseCase;
    private final PublishTypingStatusUseCase publishTypingStatusUseCase;
    private final UpdatePresenceStatusUseCase updatePresenceStatusUseCase;
    private final PublishChatListUpdateUseCase publishChatListUpdateUseCase;

    /**
     * 텍스트 채팅 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 텍스트 메시지를 데이터베이스에 저장하고,
     * 인바운드 유스케이스를 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
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

        // 브로드캐스트 (인바운드 유스케이스를 통해 처리)
        broadcastChatMessageUseCase.broadcastMessage(
                result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());

        // 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트
        publishChatListUpdateUseCase.publishChatListUpdate(result.message(), result.members(), result.senderNickname());
    }

    /**
     * 파일 첨부 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 파일 메시지를 데이터베이스에 저장하고,
     * 인바운드 유스케이스를 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
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

        // 브로드캐스트 (인바운드 유스케이스를 통해 처리)
        broadcastChatMessageUseCase.broadcastMessage(
                result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());

        // 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트
        publishChatListUpdateUseCase.publishChatListUpdate(result.message(), result.members(), result.senderNickname());
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
     * 메시지에 반응(이모지)을 추가합니다.
     *
     * <p>사용자가 특정 메시지에 이모지 반응을 추가하면 데이터베이스에 저장하고,
     * 인바운드 유스케이스를 통해 해당 채팅방의 모든 참여자에게 반응 추가 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 추가 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/reaction/add")
    public void addReaction(@Payload AddReactionRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);
        log.debug("Received reaction add from authenticated user {} to message {}", authenticatedUserId, request.messageId());

        ReactionResult result = addMessageReactionUseCase.addReactionWithContext(
                request.messageId(),
                authenticatedUserId,
                request.emoji()
        );

        // 반응 추가 이벤트를 인바운드 유스케이스를 통해 브로드캐스트
        broadcastReactionEventUseCase.broadcastReactionEvent(result.reaction(), result.chatRoomId(), "ADDED");
    }

    /**
     * 메시지에서 반응(이모지)을 제거합니다.
     *
     * <p>사용자가 특정 메시지에서 이모지 반응을 제거하면 데이터베이스에서 삭제하고,
     * 인바운드 유스케이스를 통해 해당 채팅방의 모든 참여자에게 반응 제거 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 제거 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     * @param headerAccessor WebSocket 헤더 접근자 (인증된 사용자 정보 포함)
     */
    @MessageMapping("/chat/reaction/remove")
    public void removeReaction(@Payload RemoveReactionRequest request, StompHeaderAccessor headerAccessor) {
        Long authenticatedUserId = extractUserId(headerAccessor);
        log.debug("Received reaction remove from authenticated user {} to message {}", authenticatedUserId, request.messageId());

        Long chatRoomId = removeMessageReactionUseCase.removeReactionWithContext(
                request.messageId(),
                authenticatedUserId,
                request.emoji()
        );

        if (chatRoomId == null) {
            log.warn("Cannot find chat room for message: {}", request.messageId());
            return;
        }

        // 반응 제거 이벤트를 인바운드 유스케이스를 통해 브로드캐스트
        MessageReaction removedReaction = MessageReaction.builder()
                .messageId(request.messageId())
                .userId(authenticatedUserId)
                .emoji(Emoji.fromString(request.emoji())
                        .orElseThrow(() -> new InvalidEmojiException(request.emoji())))
                .build();
        broadcastReactionEventUseCase.broadcastReactionEvent(removedReaction, chatRoomId, "REMOVED");
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
        publishTypingStatusUseCase.publishTypingStatus(
                request.roomId(), authenticatedUserId, Boolean.TRUE.equals(request.isTyping()));
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
        String sessionId = headerAccessor.getSessionId();
        updatePresenceStatusUseCase.markActive(request.roomId(), authenticatedUserId, sessionId);
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
        String sessionId = headerAccessor.getSessionId();
        updatePresenceStatusUseCase.markInactive(request.roomId(), authenticatedUserId, sessionId);
    }
}
