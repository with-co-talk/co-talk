package com.cotalk.infrastructure.config;

import com.cotalk.domain.model.Email;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 캐시 설정 클래스.
 * 애플리케이션의 캐시 매니저를 구성한다.
 *
 * <p>운영 환경에서는 Redis 캐시를 사용하여 멀티 인스턴스 환경에서
 * 캐시를 공유한다. 테스트 환경에서는 인메모리 캐시를 사용한다.</p>
 *
 * <p>지원하는 캐시:
 * <ul>
 *   <li>{@link #USER_CACHE} - 사용자 정보 캐시 (TTL: 1시간)</li>
 *   <li>{@link #CHAT_ROOM_CACHE} - 채팅방 정보 캐시 (TTL: 30분)</li>
 *   <li>{@link #STATISTICS_CACHE} - 통계 정보 캐시 (TTL: 5분)</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 사용자 정보 캐시 이름.
     */
    public static final String USER_CACHE = "users";

    /**
     * 채팅방 정보 캐시 이름.
     */
    public static final String CHAT_ROOM_CACHE = "chatRooms";

    /**
     * 통계 정보 캐시 이름.
     */
    public static final String STATISTICS_CACHE = "statistics";

    /**
     * 운영 환경용 Redis 캐시 매니저를 생성한다.
     * 멀티 인스턴스 환경에서 캐시를 공유하기 위해 Redis를 사용한다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return Redis 기반 캐시 매니저
     */
    @Bean
    @Profile("!test")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.registerModule(emailModule());
        redisObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(USER_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CHAT_ROOM_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(STATISTICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * {@link Email} 값 객체의 Redis 직렬화 모듈을 생성한다.
     * 도메인 레이어에 Jackson 의존성을 추가하지 않고, 인프라 레이어에서 처리한다.
     * {@code Email}을 plain string으로 직렬화하고, string/object 양쪽에서 역직렬화한다.
     *
     * @return Email 직렬화/역직렬화 모듈
     */
    static SimpleModule emailModule() {
        SimpleModule module = new SimpleModule("EmailModule");
        module.addSerializer(Email.class, new JsonSerializer<>() {
            @Override
            public void serialize(Email email, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(email.value());
            }
        });
        module.addDeserializer(Email.class, new JsonDeserializer<>() {
            @Override
            public Email deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException {
                return new Email(p.getText());
            }
        });
        return module;
    }

    /**
     * 테스트 환경용 캐시 매니저를 생성한다.
     * 테스트 격리를 위해 인메모리 캐시를 사용한다.
     *
     * @return 테스트용 인메모리 캐시 매니저
     */
    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                USER_CACHE,
                CHAT_ROOM_CACHE,
                STATISTICS_CACHE
        );
    }
}
