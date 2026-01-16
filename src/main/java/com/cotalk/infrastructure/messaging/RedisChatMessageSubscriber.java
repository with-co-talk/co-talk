package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Redis Pub/Sub 메시지 수신자
 * Redis에서 메시지를 수신하여 WebSocket으로 브로드캐스트
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonMessage = new String(message.getBody());
            ChatBroadcastMessage chatMessage = objectMapper.readValue(jsonMessage, ChatBroadcastMessage.class);

            // WebSocket으로 메시지 브로드캐스트
            WebSocketChatMessage wsMessage = toWebSocketMessage(chatMessage);
            String destination = "/topic/chat/room/" + chatMessage.roomId();
            
            messagingTemplate.convertAndSend(destination, wsMessage);
            log.debug("Broadcasted message to WebSocket: destination={}, messageId={}", 
                    destination, chatMessage.messageId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat message from Redis", e);
        }
    }

    private WebSocketChatMessage toWebSocketMessage(ChatBroadcastMessage msg) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.createdAtMillis()), 
                ZoneId.systemDefault()
        );
        
        return new WebSocketChatMessage(
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

    /**
     * WebSocket으로 전송할 메시지 형식
     */
    public record WebSocketChatMessage(
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
