package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.exception.MessageBrokerException;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 사용자 이벤트 브로커 구현체.
 * Redis Pub/Sub을 통해 사용자별 이벤트를 발행한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisUserEventBroker implements UserEventBroker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.user-event-prefix:user:event:}")
    private String channelPrefix;

    /**
     * 특정 사용자에게 채팅 목록 업데이트 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  채팅 목록 업데이트 이벤트
     */
    @Override
    public void publishChatListUpdate(Long userId, ChatListUpdateEvent event) {
        String channel = channelPrefix + userId + ":chat-list";
        publish(channel, event);
        log.info("Published chat list update to Redis channel {}: eventType={}, roomId={}, unreadCount={}", 
                channel, event.eventType(), event.roomId(), event.unreadCount());
    }

    /**
     * 특정 사용자에게 읽음 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  읽음 상태 이벤트
     */
    @Override
    public void publishReadReceipt(Long userId, ReadReceiptEvent event) {
        String channel = channelPrefix + userId + ":read-receipt";
        publish(channel, event);
        log.debug("Published read receipt to Redis channel {}: {}", channel, event);
    }

    /**
     * 특정 사용자에게 온라인 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  온라인 상태 이벤트
     */
    @Override
    public void publishOnlineStatus(Long userId, OnlineStatusEvent event) {
        String channel = channelPrefix + userId + ":online-status";
        publish(channel, event);
        log.info("Published online status to Redis channel {}: userId={}, isOnline={}",
                channel, event.userId(), event.isOnline());
    }

    private void publish(String channel, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for channel {}", channel, e);
            throw new MessageBrokerException("Failed to serialize user event", e);
        }
    }
}
