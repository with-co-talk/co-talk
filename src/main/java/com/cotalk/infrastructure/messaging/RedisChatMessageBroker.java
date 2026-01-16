package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub을 이용한 채팅 메시지 브로커
 * 여러 서버 인스턴스 간 메시지 브로드캐스팅을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessageBroker implements ChatMessageBroker {

    private static final String CHANNEL_PREFIX = "chat:room:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(Long roomId, ChatBroadcastMessage message) {
        String channel = CHANNEL_PREFIX + roomId;
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, jsonMessage);
            log.debug("Published message to channel {}: messageId={}", channel, message.messageId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message: {}", message, e);
            throw new RuntimeException("메시지 직렬화에 실패했습니다.", e);
        }
    }
}
