package com.cotalk.infrastructure.websocket;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtTokenProvider);
    }

    @Nested
    @DisplayName("CONNECT 명령 처리")
    class ConnectCommand {

        @Test
        @DisplayName("유효한 토큰으로 연결 성공")
        void should_allowConnection_when_validToken() {
            // given
            String token = "valid-jwt-token";
            Long userId = 1L;

            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId("test-session");
            accessor.setNativeHeader("Authorization", "Bearer " + token);
            accessor.setLeaveMutable(true);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("토큰 없이 연결 시도시 예외 발생")
        void should_throwException_when_noToken() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            // when & then
            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 토큰이 필요합니다.");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 연결 시도시 예외 발생")
        void should_throwException_when_invalidToken() {
            // given
            String token = "invalid-token";

            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setNativeHeader("Authorization", "Bearer " + token);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            given(jwtTokenProvider.validateToken(token)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 토큰입니다.");
        }

        @Test
        @DisplayName("Bearer 프리픽스 없는 토큰도 처리")
        void should_handleToken_when_noBearerPrefix() {
            // given
            String token = "valid-jwt-token";
            Long userId = 1L;

            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId("test-session");
            accessor.setNativeHeader("Authorization", token);
            accessor.setLeaveMutable(true);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("다른 STOMP 명령 처리")
    class OtherCommands {

        @Test
        @DisplayName("SEND 명령은 인증 검사 없이 통과")
        void should_passThrough_when_sendCommand() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
            accessor.setDestination("/app/chat/message");
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("SUBSCRIBE 명령은 인증 검사 없이 통과")
        void should_passThrough_when_subscribeCommand() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination("/topic/chat/room/1");
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("DISCONNECT 명령은 인증 검사 없이 통과")
        void should_passThrough_when_disconnectCommand() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }
    }
}
