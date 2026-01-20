package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * WebSocketEventListener 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener")
class WebSocketEventListenerTest {

    @Mock
    private UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    private WebSocketEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new WebSocketEventListener(updateUserOnlineStatusUseCase);
    }

    @Nested
    @DisplayName("연결 이벤트 처리 시")
    class HandleConnect {

        @Test
        @DisplayName("유효한 사용자 ID로 온라인 상태를 설정한다")
        void should_setOnline_when_validUserId() {
            // given
            SessionConnectedEvent event = createConnectedEvent("123");

            // when
            eventListener.handleWebSocketConnectListener(event);

            // then
            verify(updateUserOnlineStatusUseCase).setOnline(123L);
        }

        @Test
        @DisplayName("사용자 정보가 없으면 상태를 변경하지 않는다")
        void should_notSetOnline_when_noUser() {
            // given
            SessionConnectedEvent event = createConnectedEventWithoutUser();

            // when
            eventListener.handleWebSocketConnectListener(event);

            // then
            verifyNoInteractions(updateUserOnlineStatusUseCase);
        }

        @Test
        @DisplayName("잘못된 사용자 ID 형식이면 상태를 변경하지 않는다")
        void should_notSetOnline_when_invalidUserIdFormat() {
            // given
            SessionConnectedEvent event = createConnectedEvent("invalid");

            // when
            eventListener.handleWebSocketConnectListener(event);

            // then
            verifyNoInteractions(updateUserOnlineStatusUseCase);
        }
    }

    @Nested
    @DisplayName("해제 이벤트 처리 시")
    class HandleDisconnect {

        @Test
        @DisplayName("유효한 사용자 ID로 오프라인 상태를 설정한다")
        void should_setOffline_when_validUserId() {
            // given
            SessionDisconnectEvent event = createDisconnectEvent("456");

            // when
            eventListener.handleWebSocketDisconnectListener(event);

            // then
            verify(updateUserOnlineStatusUseCase).setOffline(456L);
        }

        @Test
        @DisplayName("사용자 정보가 없으면 상태를 변경하지 않는다")
        void should_notSetOffline_when_noUser() {
            // given
            SessionDisconnectEvent event = createDisconnectEventWithoutUser();

            // when
            eventListener.handleWebSocketDisconnectListener(event);

            // then
            verifyNoInteractions(updateUserOnlineStatusUseCase);
        }

        @Test
        @DisplayName("잘못된 사용자 ID 형식이면 상태를 변경하지 않는다")
        void should_notSetOffline_when_invalidUserIdFormat() {
            // given
            SessionDisconnectEvent event = createDisconnectEvent("not-a-number");

            // when
            eventListener.handleWebSocketDisconnectListener(event);

            // then
            verifyNoInteractions(updateUserOnlineStatusUseCase);
        }
    }

    private SessionConnectedEvent createConnectedEvent(String userId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpUser", new TestPrincipal(userId));
        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        return new SessionConnectedEvent(this, message);
    }

    private SessionConnectedEvent createConnectedEventWithoutUser() {
        Map<String, Object> headers = new HashMap<>();
        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        return new SessionConnectedEvent(this, message);
    }

    private SessionDisconnectEvent createDisconnectEvent(String userId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpUser", new TestPrincipal(userId));
        headers.put("simpSessionId", "session-123");
        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        return new SessionDisconnectEvent(this, message, "session-123", null);
    }

    private SessionDisconnectEvent createDisconnectEventWithoutUser() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", "session-123");
        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        return new SessionDisconnectEvent(this, message, "session-123", null);
    }

    private static class TestPrincipal implements Principal {
        private final String name;

        TestPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
