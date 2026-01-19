package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.exception.MessageBrokerException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 기반 채팅 메시지 브로커 구현체.
 * 여러 서버 인스턴스 간 채팅 메시지 브로드캐스팅을 담당한다.
 *
 * <p>분산 환경에서 모든 서버가 동일한 메시지를 수신할 수 있도록
 * Redis Pub/Sub 채널을 통해 메시지를 발행한다.</p>
 *
 * <p>이 컴포넌트는 {@code spring.data.redis.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessageBroker implements ChatMessageBroker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.redis.channel-prefix:chat:room:}")
    private String channelPrefix;

    /**
     * 지정된 채팅방에 메시지를 발행한다.
     * 메시지를 JSON으로 직렬화하여 Redis 채널에 발행한다.
     *
     * @param roomId 채팅방 ID
     * @param message 발행할 채팅 메시지
     * @throws MessageBrokerException 메시지 직렬화에 실패한 경우
     */
    @Override
    public void publish(Long roomId, ChatBroadcastMessage message) {
        String channel = channelPrefix + roomId;
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, jsonMessage);
            log.debug("Published message to channel {}: messageId={}", channel, message.messageId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message: {}", message, e);
            throw MessageBrokerException.serializationFailed(e);
        }
    }

    /**
     * 지정된 채팅방에 리액션 이벤트를 발행한다.
     * 리액션 이벤트를 JSON으로 직렬화하여 Redis 채널에 발행한다.
     *
     * @param roomId 채팅방 ID
     * @param reactionEvent 발행할 리액션 이벤트
     * @throws MessageBrokerException 리액션 이벤트 직렬화에 실패한 경우
     */
    @Override
    public void publishReaction(Long roomId, Object reactionEvent) {
        String channel = channelPrefix + roomId + ":reaction";
        try {
            String jsonMessage = objectMapper.writeValueAsString(reactionEvent);
            redisTemplate.convertAndSend(channel, jsonMessage);
            log.debug("Published reaction event to channel {}: {}", channel, reactionEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize reaction event: {}", reactionEvent, e);
            throw MessageBrokerException.reactionSerializationFailed(e);
        }
    }
}
