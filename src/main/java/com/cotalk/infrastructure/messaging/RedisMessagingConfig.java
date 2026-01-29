package com.cotalk.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 메시징 설정 클래스.
 * 채팅 메시지 브로드캐스팅을 위한 Redis 연결 및 리스너를 구성한다.
 *
 * <p>이 설정은 {@code spring.data.redis.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMessagingConfig {

    @Value("${app.redis.channel-prefix:chat:room:}")
    private String channelPrefix;

    @Value("${app.redis.user-event-prefix:user:event:}")
    private String userEventPrefix;

    /**
     * Redis 문자열 직렬화를 위한 RedisTemplate을 생성한다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 문자열 키/값을 사용하는 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * Redis 메시지 리스너 컨테이너를 생성한다.
     * 채팅 메시지와 사용자 이벤트 채널 패턴을 구독하여 메시지를 수신한다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @param chatMessageSubscriber 채팅 메시지 구독자
     * @param userEventSubscriber 사용자 이벤트 구독자
     * @return Redis 메시지 리스너 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisChatMessageSubscriber chatMessageSubscriber,
            RedisUserEventSubscriber userEventSubscriber) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 모든 채팅방 채널 패턴 구독 (예: chat:room:*)
        String chatChannelPattern = channelPrefix + "*";
        container.addMessageListener(chatMessageSubscriber, new PatternTopic(chatChannelPattern));
        log.info("Redis Pub/Sub listener registered for pattern: {}", chatChannelPattern);
        
        // 모든 사용자 이벤트 채널 패턴 구독 (예: user:event:*:*)
        String userEventChannelPattern = userEventPrefix + "*:*";
        container.addMessageListener(userEventSubscriber, new PatternTopic(userEventChannelPattern));
        log.info("Redis Pub/Sub listener registered for pattern: {}", userEventChannelPattern);
        
        return container;
    }

    /**
     * Java 8 시간 API를 지원하는 ObjectMapper를 생성한다.
     * ISO 8601 형식으로 날짜를 직렬화한다. (예: "2026-01-29T17:01:34")
     *
     * @return JavaTimeModule이 등록된 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // LocalDateTime을 배열이 아닌 ISO 8601 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
