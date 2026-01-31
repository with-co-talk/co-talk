package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.UserEventBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryUserEventBroker")
class InMemoryUserEventBrokerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private InMemoryUserEventBroker broker;

    @BeforeEach
    void setUp() {
        broker = new InMemoryUserEventBroker(messagingTemplate);
    }

    @Nested
    @DisplayName("publishChatListUpdate")
    class PublishChatListUpdate {

        @Test
        @DisplayName("채팅 목록 업데이트 이벤트를 WebSocket으로 발행한다")
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
                    java.time.LocalDateTime.now(),
                    300L,
                    "발신자",
                    5
            );

            // when
            broker.publishChatListUpdate(userId, event);

            // then
            ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId + "/chat-list"),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }

        @Test
        @DisplayName("다른 사용자에게도 이벤트를 발행할 수 있다")
        void should_publishToDifferentUser() {
            // given
            Long userId1 = 100L;
            Long userId2 = 200L;
            UserEventBroker.ChatListUpdateEvent event = new UserEventBroker.ChatListUpdateEvent(
                    1,
                    "event-id",
                    "MESSAGE",
                    200L,
                    "메시지 내용",
                    "TEXT",
                    java.time.LocalDateTime.now(),
                    300L,
                    "발신자",
                    5
            );

            // when
            broker.publishChatListUpdate(userId1, event);
            broker.publishChatListUpdate(userId2, event);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId1 + "/chat-list"),
                    eq(event)
            );
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId2 + "/chat-list"),
                    eq(event)
            );
        }
    }

    @Nested
    @DisplayName("publishReadReceipt")
    class PublishReadReceipt {

        @Test
        @DisplayName("읽음 상태 이벤트를 WebSocket으로 발행한다")
        void should_publishReadReceipt() {
            // given
            Long userId = 100L;
            UserEventBroker.ReadReceiptEvent event = new UserEventBroker.ReadReceiptEvent(
                    1,
                    "event-id",
                    200L,
                    300L,
                    1000L,
                    java.time.LocalDateTime.now()
            );

            // when
            broker.publishReadReceipt(userId, event);

            // then
            ArgumentCaptor<UserEventBroker.ReadReceiptEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserEventBroker.ReadReceiptEvent.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId + "/read-receipt"),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }

        @Test
        @DisplayName("여러 사용자에게 읽음 상태 이벤트를 발행할 수 있다")
        void should_publishToMultipleUsers() {
            // given
            Long userId1 = 100L;
            Long userId2 = 200L;
            UserEventBroker.ReadReceiptEvent event = new UserEventBroker.ReadReceiptEvent(
                    1,
                    "event-id",
                    200L,
                    300L,
                    1000L,
                    java.time.LocalDateTime.now()
            );

            // when
            broker.publishReadReceipt(userId1, event);
            broker.publishReadReceipt(userId2, event);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId1 + "/read-receipt"),
                    eq(event)
            );
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/" + userId2 + "/read-receipt"),
                    eq(event)
            );
        }
    }
}
