package com.cotalk.infrastructure.messaging;

import com.cotalk.adapter.inbound.websocket.dto.WebSocketMessage;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 인메모리 기반 채팅 메시지 브로커 구현체.
 * Redis가 비활성화된 단일 서버 환경에서 사용된다.
 *
 * <p>개발 및 테스트 환경에서 Redis 없이 채팅 기능을 테스트할 수 있도록 한다.
 * 이 컴포넌트는 {@code spring.data.redis.enabled=false}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class InMemoryChatMessageBroker implements ChatMessageBroker {

    private final SimpMessagingTemplate messagingTemplate;
    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/room/";

    /**
     * 지정된 채팅방에 메시지를 발행한다.
     * 인메모리 구현이므로 직접 WebSocket으로 브로드캐스트한다.
     *
     * @param roomId 채팅방 ID
     * @param message 발행할 채팅 메시지
     */
    @Override
    public void publish(Long roomId, ChatBroadcastMessage message) {
        log.debug("InMemory broadcast to room {}: messageId={}", roomId, message.messageId());

        // 직접 WebSocket으로 브로드캐스트 (단일 서버 환경)
        WebSocketMessage wsMessage = toWebSocketMessage(message);
        String destination = ROOM_TOPIC_PREFIX + roomId;
        
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    /**
     * ChatBroadcastMessage를 WebSocket 전송용 메시지로 변환한다.
     *
     * @param msg 변환할 채팅 브로드캐스트 메시지
     * @return WebSocket 전송용 메시지
     */
    private WebSocketMessage toWebSocketMessage(ChatBroadcastMessage msg) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.createdAtMillis()),
                ZoneId.systemDefault()
        );

        return new WebSocketMessage(
                1,
                "message:" + msg.messageId(),
                msg.messageId(),
                msg.senderId(),
                msg.roomId(),
                // 과거 데이터 호환: 저장된 HTML 엔티티를 복원해 클라이언트에 원문으로 보여준다.
                HtmlSanitizer.unescape(msg.content()),
                msg.type(),
                createdAt,
                msg.fileUrl(),
                msg.fileName(),
                msg.fileSize(),
                msg.contentType(),
                msg.thumbnailUrl(),
                msg.unreadCount()
        );
    }

    /**
     * 지정된 채팅방에 리액션 이벤트를 발행한다.
     * 인메모리 구현이므로 직접 WebSocket으로 브로드캐스트한다.
     *
     * @param roomId 채팅방 ID
     * @param reactionEvent 발행할 리액션 이벤트
     */
    @Override
    public void publishReaction(Long roomId, Object reactionEvent) {
        log.debug("InMemory broadcast reaction to room {}: {}", roomId, reactionEvent);
        
        // 직접 WebSocket으로 브로드캐스트 (단일 서버 환경)
        String destination = ROOM_TOPIC_PREFIX + roomId + "/reaction";
        messagingTemplate.convertAndSend(destination, reactionEvent);
    }

    /**
     * 지정된 채팅방에 이벤트를 발행한다.
     * 인메모리 구현이므로 직접 WebSocket(/topic/chat/room/{roomId})으로 브로드캐스트한다.
     *
     * @param roomId 채팅방 ID
     * @param event  발행할 이벤트 객체
     */
    @Override
    public void publishRoomEvent(Long roomId, Object event) {
        log.debug("InMemory broadcast room event to room {}: {}", roomId, event);
        String destination = ROOM_TOPIC_PREFIX + roomId;
        messagingTemplate.convertAndSend(destination, event);
    }
}
