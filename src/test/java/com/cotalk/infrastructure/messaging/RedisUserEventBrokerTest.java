package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.exception.MessageBrokerException;
import com.cotalk.domain.port.outbound.UserEventBroker;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisUserEventBroker")
class RedisUserEventBrokerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private ObjectMapper objectMapper;
    private RedisUserEventBroker broker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        broker = new RedisUserEventBroker(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(broker, "channelPrefix", "user:event:");
    }

    @Nested
    @DisplayName("publishChatListUpdate")
    class PublishChatListUpdate {

        @Test
        @DisplayName("채팅 목록 업데이트 이벤트를 Redis로 발행한다")
        void should_publishChatListUpdate() {
            // given
            Long userId = 100L;
            UserEventBroker.ChatListUpdateEvent event = new UserEventBroker.ChatListUpdateEvent(
                    1,
                    "event-id",
                    "MESSAGE",
                    200L,
                    "메시지 내용",
                    "TEXT",
                    LocalDateTime.now(),
                    300L,
                    "발신자",
                    5
            );

            // when
            broker.publishChatListUpdate(userId, event);

            // then
            ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

            assertThat(channelCaptor.getValue()).isEqualTo("user:event:100:chat-list");
            assertThat(messageCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("JSON 직렬화 실패 시 예외 발생")
        void should_throwException_when_serializationFails() throws JsonProcessingException {
            // given
            Long userId = 100L;
            UserEventBroker.ChatListUpdateEvent event = new UserEventBroker.ChatListUpdateEvent(
                    1,
                    "event-id",
                    "MESSAGE",
                    200L,
                    "메시지 내용",
                    "TEXT",
                    LocalDateTime.now(),
                    300L,
                    "발신자",
                    5
            );

            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test") {});

            RedisUserEventBroker failingBroker = new RedisUserEventBroker(redisTemplate, failingMapper);
            ReflectionTestUtils.setField(failingBroker, "channelPrefix", "user:event:");

            // when & then
            assertThatThrownBy(() -> failingBroker.publishChatListUpdate(userId, event))
                    .isInstanceOf(MessageBrokerException.class)
                    .hasMessageContaining("Failed to serialize user event");
        }
    }

    @Nested
    @DisplayName("publishReadReceipt")
    class PublishReadReceipt {

        @Test
        @DisplayName("읽음 상태 이벤트를 Redis로 발행한다")
        void should_publishReadReceipt() {
            // given
            Long userId = 100L;
            UserEventBroker.ReadReceiptEvent event = new UserEventBroker.ReadReceiptEvent(
                    1,
                    "event-id",
                    200L,
                    300L,
                    1000L,
                    LocalDateTime.now()
            );

            // when
            broker.publishReadReceipt(userId, event);

            // then
            ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

            assertThat(channelCaptor.getValue()).isEqualTo("user:event:100:read-receipt");
            assertThat(messageCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("JSON 직렬화 실패 시 예외 발생")
        void should_throwException_when_serializationFails() throws JsonProcessingException {
            // given
            Long userId = 100L;
            UserEventBroker.ReadReceiptEvent event = new UserEventBroker.ReadReceiptEvent(
                    1,
                    "event-id",
                    200L,
                    300L,
                    1000L,
                    LocalDateTime.now()
            );

            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test") {});

            RedisUserEventBroker failingBroker = new RedisUserEventBroker(redisTemplate, failingMapper);
            ReflectionTestUtils.setField(failingBroker, "channelPrefix", "user:event:");

            // when & then
            assertThatThrownBy(() -> failingBroker.publishReadReceipt(userId, event))
                    .isInstanceOf(MessageBrokerException.class)
                    .hasMessageContaining("Failed to serialize user event");
        }
    }
}
