package com.cotalk.infrastructure.messaging;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisMessagingConfig 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisMessagingConfig")
class RedisMessagingConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisChatMessageSubscriber subscriber;

    @Mock
    private RedisUserEventSubscriber userEventSubscriber;

    private AppProperties createTestAppProperties() {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption("", true),
                new AppProperties.Swagger("http://localhost:8080", "API 서버"),
                new AppProperties.Search("dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM=")
        );
    }

    @Nested
    @DisplayName("RedisTemplate 생성 시")
    class RedisTemplateCreation {

        @Test
        @DisplayName("문자열 직렬화를 사용하는 RedisTemplate을 생성한다")
        void should_createRedisTemplate_withStringSerializer() {
            // given
            RedisMessagingConfig config = new RedisMessagingConfig(createTestAppProperties());

            // when
            RedisTemplate<String, String> template = config.redisTemplate(connectionFactory);

            // then
            assertThat(template).isNotNull();
            assertThat(template.getConnectionFactory()).isEqualTo(connectionFactory);
            assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(template.getHashValueSerializer()).isInstanceOf(StringRedisSerializer.class);
        }
    }

    @Nested
    @DisplayName("RedisMessageListenerContainer 생성 시")
    class ListenerContainerCreation {

        @Test
        @DisplayName("메시지 리스너 컨테이너를 생성한다")
        void should_createListenerContainer() {
            // given
            RedisMessagingConfig config = new RedisMessagingConfig(createTestAppProperties());

            // when
            RedisMessageListenerContainer container = config.redisMessageListenerContainer(
                    connectionFactory, subscriber, userEventSubscriber);

            // then
            assertThat(container).isNotNull();
            assertThat(container.getConnectionFactory()).isEqualTo(connectionFactory);
        }
    }

    @Nested
    @DisplayName("ObjectMapper 생성 시")
    class ObjectMapperCreation {

        @Test
        @DisplayName("JavaTimeModule이 등록된 ObjectMapper를 생성한다")
        void should_createObjectMapper_withJavaTimeModule() {
            // given
            RedisMessagingConfig config = new RedisMessagingConfig(createTestAppProperties());

            // when
            ObjectMapper mapper = config.objectMapper();

            // then
            assertThat(mapper).isNotNull();
            // JavaTimeModule이 등록되었는지 확인
            assertThat(mapper.getRegisteredModuleIds())
                    .anyMatch(id -> id.toString().contains("jackson-datatype-jsr310"));
        }

        @Test
        @DisplayName("ObjectMapper가 LocalDateTime을 직렬화할 수 있다")
        void should_serializeLocalDateTime() throws Exception {
            // given
            RedisMessagingConfig config = new RedisMessagingConfig(createTestAppProperties());
            ObjectMapper mapper = config.objectMapper();
            java.time.LocalDateTime now = java.time.LocalDateTime.of(2024, 1, 1, 12, 0, 0);

            // when
            String json = mapper.writeValueAsString(now);

            // then
            assertThat(json).isNotNull();
            assertThat(json).contains("2024");
        }
    }
}
