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
 * Redis Pub/Sub 메시지 구독자.
 * Redis 채널에서 채팅 메시지를 수신하여 WebSocket을 통해 클라이언트에게 브로드캐스트한다.
 *
 * <p>이 컴포넌트는 {@code spring.data.redis.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis로부터 메시지를 수신하여 처리한다.
     * 수신된 JSON 메시지를 역직렬화하고 WebSocket으로 브로드캐스트한다.
     *
     * @param message Redis로부터 수신한 메시지
     * @param pattern 매칭된 채널 패턴 (바이트 배열)
     */
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

    /**
     * ChatBroadcastMessage를 WebSocket 전송용 메시지로 변환한다.
     *
     * @param msg 변환할 채팅 브로드캐스트 메시지
     * @return WebSocket 전송용 채팅 메시지
     */
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
     * WebSocket으로 전송할 채팅 메시지 DTO.
     *
     * @param messageId 메시지 ID
     * @param senderId 발신자 ID
     * @param roomId 채팅방 ID
     * @param content 메시지 내용
     * @param type 메시지 타입
     * @param createdAt 생성 일시
     * @param fileUrl 파일 URL (파일 메시지인 경우)
     * @param fileName 파일명 (파일 메시지인 경우)
     * @param fileSize 파일 크기 (파일 메시지인 경우)
     * @param contentType 컨텐츠 타입 (파일 메시지인 경우)
     * @param thumbnailUrl 썸네일 URL (이미지 메시지인 경우)
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
