package com.cotalk.infrastructure.messaging;

import com.cotalk.adapter.inbound.websocket.dto.ReactionBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.util.HtmlSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/room/";

    /**
     * Redis 채널 prefix.
     * <p>
     * 단위 테스트(스프링 컨텍스트 없음)에서도 안전하게 동작하도록 기본값을 코드 레벨로 보장한다.
     */
    @Value("${app.redis.channel-prefix:chat:room:}")
    private String channelPrefix = "chat:room:";

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
            String channel = new String(message.getChannel());
            String jsonMessage = new String(message.getBody());

            RoomChannelInfo channelInfo = RoomChannelInfo.parse(channelPrefix, channel);
            if (channelInfo == null) {
                log.warn("Invalid chat room channel format: {}", channel);
                return;
            }

            // suffix 기반 분기:
            // - chat:room:{roomId}            -> 채팅 메시지
            // - chat:room:{roomId}:reaction   -> 리액션 이벤트
            // - chat:room:{roomId}:event      -> 채팅방 이벤트(READ 등)
            if (channelInfo.isReactionChannel()) {
                handleReaction(channelInfo.roomId(), jsonMessage);
                return;
            }
            if (channelInfo.isEventChannel()) {
                handleRoomEvent(channelInfo.roomId(), jsonMessage);
                return;
            }

            ChatBroadcastMessage chatMessage = objectMapper.readValue(jsonMessage, ChatBroadcastMessage.class);
            WebSocketChatMessage wsMessage = toWebSocketMessage(chatMessage);
            String destination = ROOM_TOPIC_PREFIX + chatMessage.roomId();
            messagingTemplate.convertAndSend(destination, wsMessage);
            log.debug("Broadcasted message to WebSocket: destination={}, messageId={}",
                    destination, chatMessage.messageId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat message from Redis", e);
        } catch (Exception e) {
            log.error("Failed to process chat message from Redis", e);
        }
    }

    private void handleReaction(Long roomId, String jsonMessage) throws JsonProcessingException {
        ReactionBroadcastMessage reactionEvent = objectMapper.readValue(jsonMessage, ReactionBroadcastMessage.class);
        String destination = ROOM_TOPIC_PREFIX + roomId + "/reaction";
        messagingTemplate.convertAndSend(destination, reactionEvent);
        log.debug("Broadcasted reaction to WebSocket: destination={}, messageId={}",
                destination, reactionEvent.messageId());
    }

    private void handleRoomEvent(Long roomId, String jsonMessage) throws JsonProcessingException {
        ChatRoomEventMessage event = objectMapper.readValue(jsonMessage, ChatRoomEventMessage.class);
        String destination = ROOM_TOPIC_PREFIX + roomId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcasted room event to WebSocket: destination={}, eventType={}",
                destination, event.eventType());
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
                1,
                "message:" + msg.messageId() + ":" + System.currentTimeMillis(),
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
     * WebSocket으로 전송할 채팅 메시지 DTO.
     *
     * @param messageId    메시지 ID
     * @param senderId     발신자 ID
     * @param roomId       채팅방 ID
     * @param content      메시지 내용
     * @param type         메시지 타입
     * @param createdAt    생성 일시
     * @param fileUrl      파일 URL (파일 메시지인 경우)
     * @param fileName     파일명 (파일 메시지인 경우)
     * @param fileSize     파일 크기 (파일 메시지인 경우)
     * @param contentType  컨텐츠 타입 (파일 메시지인 경우)
     * @param thumbnailUrl 썸네일 URL (이미지 메시지인 경우)
     * @param unreadCount  읽지 않은 멤버 수 (발신자 제외)
     */
    public record WebSocketChatMessage(
            Integer schemaVersion,
            String eventId,
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
            String thumbnailUrl,
            Integer unreadCount
    ) {}

    /**
     * 채팅방 이벤트 메시지 DTO.
     * 채팅방 단위 토픽(/topic/chat/room/{roomId})으로 발행되는 이벤트(READ 등)에 사용한다.
     *
     * @param eventType  이벤트 타입 (예: READ)
     * @param chatRoomId 채팅방 ID
     * @param userId     이벤트를 발생시킨 사용자 ID (예: reader)
     * @param lastReadAt 마지막 읽은 시간 (optional)
     */
    public record ChatRoomEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            Long lastReadMessageId,
            LocalDateTime lastReadAt
    ) {}

    /**
     * chat:room:{roomId}(:suffix) 채널 파서.
     */
    private record RoomChannelInfo(Long roomId, String suffix) {
        static RoomChannelInfo parse(String prefix, String fullChannel) {
            if (fullChannel == null || prefix == null || !fullChannel.startsWith(prefix)) {
                return null;
            }
            String rest = fullChannel.substring(prefix.length()); // e.g. "10" or "10:reaction"
            String[] parts = rest.split(":");
            if (parts.length == 0) return null;
            Long roomId;
            try {
                roomId = Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                return null;
            }
            String suffix = parts.length >= 2 ? parts[1] : null;
            return new RoomChannelInfo(roomId, suffix);
        }

        boolean isReactionChannel() {
            return "reaction".equals(suffix);
        }

        boolean isEventChannel() {
            return "event".equals(suffix);
        }
    }
}
