package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 개발/테스트용 인메모리 채팅 메시지 브로커
 * Redis가 비활성화되었을 때 사용 (단일 서버 환경)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class InMemoryChatMessageBroker implements ChatMessageBroker {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Long roomId, ChatBroadcastMessage message) {
        log.debug("InMemory broadcast to room {}: messageId={}", roomId, message.messageId());

        // 직접 WebSocket으로 브로드캐스트 (단일 서버 환경)
        WebSocketMessage wsMessage = toWebSocketMessage(message);
        String destination = "/topic/chat/room/" + roomId;
        
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    private WebSocketMessage toWebSocketMessage(ChatBroadcastMessage msg) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.createdAtMillis()),
                ZoneId.systemDefault()
        );

        return new WebSocketMessage(
                msg.messageId(),
                msg.senderId(),
                msg.roomId(),
                msg.content(),
                msg.type(),
                createdAt,
                msg.fileUrl(),
                msg.fileName(),
                msg.fileSize(),
                msg.contentType(),
                msg.thumbnailUrl()
        );
    }

    public record WebSocketMessage(
            Long messageId,
            Long senderId,
            Long roomId,
            String content,
            String type,
            LocalDateTime createdAt,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {}
}
