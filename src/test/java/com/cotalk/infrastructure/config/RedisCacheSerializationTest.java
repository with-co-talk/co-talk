package com.cotalk.infrastructure.config;

import com.cotalk.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 캐시 값 직렬화/역직렬화 검증 테스트.
 * <p>
 * 운영 환경의 CacheConfig는 {@code @Profile("!test")}로 테스트에서 로드되지 않아
 * Redis용 GenericJackson2JsonRedisSerializer 경로가 테스트되지 않는다.
 * 이 테스트는 "User 등 LocalDateTime을 가진 엔티티가 Redis 캐시로 직렬화 가능한지"를
 * 명시적으로 검증하여, 런타임 SerializationException을 방지한다.
 *
 * @author seunggu.lee
 */
@DisplayName("Redis 캐시 직렬화")
class RedisCacheSerializationTest {

    private GenericJackson2JsonRedisSerializer valueSerializer;

    @BeforeEach
    void setUp() {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        valueSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Test
    @DisplayName("User 엔티티(LocalDateTime 포함)를 직렬화·역직렬화할 수 있다")
    void should_serializeAndDeserializeUser_withLocalDateTime() throws Exception {
        // given: BaseEntity의 createdAt/updatedAt, User의 lastActiveAt이 있는 User
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(1L)
                .email("serialize-test@example.com")
                .nickname("serializeTest")
                .status(User.UserStatus.ACTIVE)
                .role(User.Role.USER)
                .onlineStatus(User.OnlineStatus.OFFLINE)
                .lastActiveAt(now.minusHours(1))
                .build();
        setBaseEntityAuditFields(user, now.minusDays(1), now);

        // when: Redis 캐시와 동일한 방식으로 직렬화 후 역직렬화
        byte[] bytes = valueSerializer.serialize(user);
        Object deserialized = valueSerializer.deserialize(bytes);

        // then
        assertThat(deserialized).isInstanceOf(User.class);
        User restored = (User) deserialized;
        assertThat(restored.getId()).isEqualTo(user.getId());
        assertThat(restored.getEmail()).isEqualTo(user.getEmail());
        assertThat(restored.getLastActiveAt()).isEqualTo(user.getLastActiveAt());
        assertThat(restored.getCreatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(restored.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
    }

    @Test
    @DisplayName("JavaTimeModule 없이 User를 직렬화하면 예외가 발생한다 (회귀 방지)")
    void should_throwWhenSerializingUser_withoutJavaTimeModule() {
        // given: JavaTimeModule이 없는 ObjectMapper (과거 CacheConfig와 동일)
        GenericJackson2JsonRedisSerializer serializerWithoutJsr310 =
                new GenericJackson2JsonRedisSerializer(new ObjectMapper());

        User user = User.builder()
                .id(2L)
                .email("fail@example.com")
                .nickname("fail")
                .status(User.UserStatus.ACTIVE)
                .role(User.Role.USER)
                .onlineStatus(User.OnlineStatus.OFFLINE)
                .build();
        try {
            setBaseEntityAuditFields(user, LocalDateTime.now(), LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // when & then: LocalDateTime 직렬화 시 SerializationException
        org.junit.jupiter.api.Assertions.assertThrows(
                SerializationException.class,
                () -> serializerWithoutJsr310.serialize(user)
        );
    }

    private void setBaseEntityAuditFields(User user, LocalDateTime createdAt, LocalDateTime updatedAt)
            throws Exception {
        Field createdAtField = com.cotalk.domain.entity.BaseEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(user, createdAt);
        Field updatedAtField = com.cotalk.domain.entity.BaseEntity.class.getDeclaredField("updatedAt");
        updatedAtField.setAccessible(true);
        updatedAtField.set(user, updatedAt);
    }
}
