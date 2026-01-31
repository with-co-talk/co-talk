package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.UserEventBroker;
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
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisUserEventSubscriber")
class RedisUserEventSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private RedisUserEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        subscriber = new RedisUserEventSubscriber(messagingTemplate, objectMapper);
        ReflectionTestUtils.setField(subscriber, "channelPrefix", "user:event:");
    }

    private Message createRedisMessage(String body, String channel) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getChannel() {
                return channel.getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    @Nested
    @DisplayName("읽음 상태 이벤트 수신")
    class ReadReceiptEvent {

        @Test
        @DisplayName("읽음 상태 이벤트를 수신하여 WebSocket으로 브로드캐스트한다")
        void should_broadcastReadReceipt_when_eventReceived() throws Exception {
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
            String jsonMessage = objectMapper.writeValueAsString(event);
            String channel = "user:event:100:read-receipt";
            Message redisMessage = createRedisMessage(jsonMessage, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            ArgumentCaptor<UserEventBroker.ReadReceiptEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserEventBroker.ReadReceiptEvent.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/100/read-receipt"),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(200L);
            assertThat(eventCaptor.getValue().userId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("잘못된 JSON 형식의 읽음 상태 이벤트는 무시한다")
        void should_ignoreInvalidJson_when_readReceiptEvent() {
            // given
            String invalidJson = "{ invalid json }";
            String channel = "user:event:100:read-receipt";
            Message redisMessage = createRedisMessage(invalidJson, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("채팅 목록 업데이트 이벤트 수신")
    class ChatListUpdateEvent {

        @Test
        @DisplayName("채팅 목록 업데이트 이벤트를 수신하여 WebSocket으로 브로드캐스트한다")
        void should_broadcastChatListUpdate_when_eventReceived() throws Exception {
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
            String jsonMessage = objectMapper.writeValueAsString(event);
            String channel = "user:event:100:chat-list";
            Message redisMessage = createRedisMessage(jsonMessage, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/100/chat-list"),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue().roomId()).isEqualTo(200L);
            assertThat(eventCaptor.getValue().unreadCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("잘못된 JSON 형식의 채팅 목록 업데이트 이벤트는 무시한다")
        void should_ignoreInvalidJson_when_chatListUpdateEvent() {
            // given
            String invalidJson = "{ invalid json }";
            String channel = "user:event:100:chat-list";
            Message redisMessage = createRedisMessage(invalidJson, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("잘못된 채널 형식")
    class InvalidChannelFormat {

        @Test
        @DisplayName("잘못된 채널 형식은 무시한다")
        void should_ignoreInvalidChannelFormat() {
            // given
            String channel = "invalid:channel:format";
            String jsonMessage = "{}";
            Message redisMessage = createRedisMessage(jsonMessage, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("알 수 없는 이벤트 타입은 무시한다")
        void should_ignoreUnknownEventType() {
            // given
            String channel = "user:event:100:unknown-event";
            String jsonMessage = "{}";
            Message redisMessage = createRedisMessage(jsonMessage, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("채널에서 userId를 파싱할 수 없으면 무시한다")
        void should_ignoreInvalidUserId() {
            // given
            String channel = "user:event:invalid:read-receipt";
            String jsonMessage = "{}";
            Message redisMessage = createRedisMessage(jsonMessage, channel);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }
}
