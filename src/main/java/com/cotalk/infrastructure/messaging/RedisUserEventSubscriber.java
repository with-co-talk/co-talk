package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.infrastructure.config.properties.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 사용자 이벤트 구독자.
 * Redis 채널에서 사용자 이벤트(읽음 상태, 채팅 목록 업데이트)를 수신하여 WebSocket을 통해 클라이언트에게 브로드캐스트한다.
 *
 * <p>이 컴포넌트는 {@code spring.data.redis.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisUserEventSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String channelPrefix;

    /**
     * RedisUserEventSubscriber 생성자.
     *
     * @param messagingTemplate WebSocket 메시지 전송용 템플릿
     * @param objectMapper      JSON 역직렬화용 ObjectMapper
     * @param appProperties     앱 설정 프로퍼티
     */
    public RedisUserEventSubscriber(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.channelPrefix = appProperties.redis().userEventPrefix();
    }

    /**
     * Redis로부터 사용자 이벤트를 수신하여 처리한다.
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

            // 채널 이름에서 사용자 ID와 이벤트 타입 추출
            // 예: user:event:123:read-receipt -> userId=123, eventType=read-receipt
            String[] parts = channel.replace(channelPrefix, "").split(":");
            if (parts.length < 2) {
                log.warn("Invalid channel format: {}", channel);
                return;
            }

            Long userId = Long.parseLong(parts[0]);
            String eventType = parts[1];

            // 이벤트 타입에 따라 처리
            if ("read-receipt".equals(eventType)) {
                handleReadReceipt(userId, jsonMessage);
            } else if ("chat-list".equals(eventType)) {
                handleChatListUpdate(userId, jsonMessage);
            } else if ("online-status".equals(eventType)) {
                handleOnlineStatus(userId, jsonMessage);
            } else {
                log.warn("Unknown event type: {} from channel: {}", eventType, channel);
            }

        } catch (Exception e) {
            log.error("Failed to process user event from Redis", e);
        }
    }

    /**
     * 읽음 상태 이벤트를 처리한다.
     *
     * @param userId      대상 사용자 ID
     * @param jsonMessage JSON 메시지
     */
    private void handleReadReceipt(Long userId, String jsonMessage) {
        try {
            UserEventBroker.ReadReceiptEvent event = objectMapper.readValue(
                    jsonMessage, 
                    UserEventBroker.ReadReceiptEvent.class
            );

            String destination = "/topic/user/" + userId + "/read-receipt";
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted read receipt to WebSocket: destination={}, chatRoomId={}, userId={}", 
                    destination, event.chatRoomId(), event.userId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize read receipt event", e);
        }
    }

    /**
     * 채팅 목록 업데이트 이벤트를 처리한다.
     *
     * @param userId      대상 사용자 ID
     * @param jsonMessage JSON 메시지
     */
    private void handleChatListUpdate(Long userId, String jsonMessage) {
        try {
            UserEventBroker.ChatListUpdateEvent event = objectMapper.readValue(
                    jsonMessage,
                    UserEventBroker.ChatListUpdateEvent.class
            );

            String destination = "/topic/user/" + userId + "/chat-list";
            messagingTemplate.convertAndSend(destination, event);
            log.info("Broadcasted chat list update to WebSocket: destination={}, eventType={}, roomId={}, unreadCount={}",
                    destination, event.eventType(), event.roomId(), event.unreadCount());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat list update event", e);
        }
    }

    /**
     * 온라인 상태 이벤트를 처리한다.
     *
     * @param userId      대상 사용자 ID
     * @param jsonMessage JSON 메시지
     */
    private void handleOnlineStatus(Long userId, String jsonMessage) {
        try {
            UserEventBroker.OnlineStatusEvent event = objectMapper.readValue(
                    jsonMessage,
                    UserEventBroker.OnlineStatusEvent.class
            );

            String destination = "/topic/user/" + userId + "/online-status";
            messagingTemplate.convertAndSend(destination, event);
            log.info("Broadcasted online status to WebSocket: destination={}, targetUserId={}, isOnline={}",
                    destination, event.userId(), event.isOnline());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize online status event", e);
        }
    }
}
