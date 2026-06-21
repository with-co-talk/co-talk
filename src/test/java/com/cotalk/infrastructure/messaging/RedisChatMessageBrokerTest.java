package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.exception.MessageBrokerException;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ReactionBroadcastEvent;
import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.metrics.CustomMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RedisChatMessageBroker 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisChatMessageBroker")
class RedisChatMessageBrokerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private CustomMetrics customMetrics;

    private ObjectMapper objectMapper;
    private RedisChatMessageBroker broker;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        appProperties = createTestAppProperties();
        broker = new RedisChatMessageBroker(redisTemplate, objectMapper, appProperties, customMetrics);
    }

    private AppProperties createTestAppProperties() {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption("", true),
                new AppProperties.Swagger("http://localhost:8080", "API 서버"),
                AppProperties.Search.of("dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM="),
                new AppProperties.Lock(false)
        );
    }

    @Nested
    @DisplayName("메시지 발행 시")
    class Publish {

        @Test
        @DisplayName("메시지 발행 시 Redis 채널로 전송")
        void should_publishToRedisChannel_when_publish() {
            // given
            Long roomId = 100L;
            ChatBroadcastMessage message = new ChatBroadcastMessage(
                    1L,           // messageId
                    10L,          // senderId
                    "테스트유저", // senderNickname
                    "https://example.com/avatar.jpg", // senderAvatarUrl
                    roomId,       // roomId
                    "Hello!",     // content
                    "TEXT",       // type
                    System.currentTimeMillis(),
                    null, null, null, null, null,  // file fields
                    1,            // unreadCount
                    null, null, null,  // eventType, relatedUserId, relatedUserNickname
                    null  // clientMessageId
            );

            // when
            broker.publish(roomId, message);

            // then
            ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

            assertThat(channelCaptor.getValue()).isEqualTo("chat:room:100");
            assertThat(messageCaptor.getValue()).contains("Hello!");
            assertThat(messageCaptor.getValue()).contains("\"messageId\":1");
        }

        @Test
        @DisplayName("파일 메시지 발행")
        void should_publishFileMessage_when_fileMessagePublish() {
            // given
            Long roomId = 100L;
            ChatBroadcastMessage message = new ChatBroadcastMessage(
                    1L,
                    10L,
                    "테스트유저", // senderNickname
                    "https://example.com/avatar.jpg", // senderAvatarUrl
                    roomId,
                    "photo.jpg",
                    "IMAGE",
                    System.currentTimeMillis(),
                    "https://storage.example.com/photo.jpg",
                    "photo.jpg",
                    102400L,
                    "image/jpeg",
                    "https://storage.example.com/thumb.jpg",
                    1,  // unreadCount
                    null, null, null,  // eventType, relatedUserId, relatedUserNickname
                    null  // clientMessageId
            );

            // when
            broker.publish(roomId, message);

            // then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).convertAndSend(eq("chat:room:100"), messageCaptor.capture());

            assertThat(messageCaptor.getValue()).contains("IMAGE");
            assertThat(messageCaptor.getValue()).contains("photo.jpg");
            assertThat(messageCaptor.getValue()).contains("https://storage.example.com/photo.jpg");
        }

        @Test
        @DisplayName("직렬화 실패 시 MessageBrokerException을 던진다")
        void should_throwException_when_serializationFails() throws Exception {
            // given
            ObjectMapper mockMapper = mock(ObjectMapper.class);
            RedisChatMessageBroker brokerWithMockMapper = new RedisChatMessageBroker(redisTemplate, mockMapper, appProperties, customMetrics);

            Long roomId = 1L;
            ChatBroadcastMessage message = new ChatBroadcastMessage(
                    100L, 1L, "테스트유저", null, roomId, "test", "TEXT",
                    System.currentTimeMillis(), null, null, null, null, null, 1,
                    null, null, null, null
            );
            when(mockMapper.writeValueAsString(message))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // when & then
            assertThatThrownBy(() -> brokerWithMockMapper.publish(roomId, message))
                    .isInstanceOf(MessageBrokerException.class);
        }
    }

    @Nested
    @DisplayName("리액션 발행 시")
    class PublishReaction {

        @Test
        @DisplayName("리액션 이벤트를 Redis 채널에 발행한다")
        void should_publishReaction_when_validInput() {
            // given
            Long roomId = 1L;
            ReactionBroadcastEvent reactionEvent = new ReactionBroadcastEvent(
                    1, "event:100:1:ADDED", 1L, 100L, 1L, "👍", "ADDED", System.currentTimeMillis());

            // when
            broker.publishReaction(roomId, reactionEvent);

            // then
            ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

            assertThat(channelCaptor.getValue()).isEqualTo("chat:room:1:reaction");
            assertThat(messageCaptor.getValue()).contains("ADDED");
            assertThat(messageCaptor.getValue()).contains("100");
        }

        @Test
        @DisplayName("리액션 직렬화 실패 시 MessageBrokerException을 던진다")
        void should_throwException_when_reactionSerializationFails() throws Exception {
            // given
            ObjectMapper mockMapper = mock(ObjectMapper.class);
            RedisChatMessageBroker brokerWithMockMapper = new RedisChatMessageBroker(redisTemplate, mockMapper, appProperties, customMetrics);

            Long roomId = 1L;
            ReactionBroadcastEvent reactionEvent = new ReactionBroadcastEvent(
                    1, "event:100:1:ADDED", 1L, 100L, 1L, "👍", "ADDED", System.currentTimeMillis());
            when(mockMapper.writeValueAsString(reactionEvent))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // when & then
            assertThatThrownBy(() -> brokerWithMockMapper.publishReaction(roomId, reactionEvent))
                    .isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("다양한 채팅방에 리액션을 발행할 수 있다")
        void should_publishToCorrectChannel_when_differentRoomIds() {
            // given
            Long roomId = 456L;
            ReactionBroadcastEvent reactionEvent = new ReactionBroadcastEvent(
                    1, "event:200:2:ADDED", 2L, 200L, 2L, "❤️", "ADDED", System.currentTimeMillis());

            // when
            broker.publishReaction(roomId, reactionEvent);

            // then
            verify(redisTemplate).convertAndSend(eq("chat:room:456:reaction"), anyString());
        }
    }

}
