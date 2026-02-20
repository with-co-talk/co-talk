package com.cotalk.infrastructure.websocket;

import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class WebSocketConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Inbound ports
    @MockitoBean
    private SignUpUseCase signUpUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private SendMessageUseCase sendMessageUseCase;

    @MockitoBean
    private GetMessageHistoryUseCase getMessageHistoryUseCase;

    @MockitoBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockitoBean
    private SendFriendRequestUseCase sendFriendRequestUseCase;

    @MockitoBean
    private AcceptFriendRequestUseCase acceptFriendRequestUseCase;

    @MockitoBean
    private GetFriendListUseCase getFriendListUseCase;

    // Outbound ports
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MessageRepository messageRepository;

    @MockitoBean
    private ChatRoomRepository chatRoomRepository;

    @MockitoBean
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @MockitoBean
    private FriendRepository friendRepository;

    @MockitoBean
    private FriendRequestRepository friendRequestRepository;

    // Infrastructure
    @MockitoBean
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    @DisplayName("유효한 토큰으로 WebSocket 연결 성공")
    void should_connectSuccessfully_when_validToken() throws Exception {
        // given
        String token = jwtTokenProvider.generateToken(1L);
        
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();

        // when
        stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        sessionFuture.complete(session);
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {
                        sessionFuture.completeExceptionally(exception);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        sessionFuture.completeExceptionally(exception);
                    }
                });

        // then
        StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);
        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 WebSocket 연결 시 실패한다")
    void should_failConnection_when_invalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + invalidToken);

        CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();
        stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        errorFuture.complete(exception);
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {
                        errorFuture.complete(exception);
                    }
                }
        );

        Throwable t = errorFuture.get(5, TimeUnit.SECONDS);
        assertThat(t).isNotNull();
        String message = t.getMessage() != null ? t.getMessage() : (t.getCause() != null ? t.getCause().getMessage() : "");
        assertThat(message.contains("유효하지 않은") || message.contains("Connection closed")).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰으로 채팅방 구독 성공")
    void should_subscribeSuccessfully_when_validToken() throws Exception {
        // given: room 1 멤버로 user 1 허용 (인터셉터 권한 검사)
        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(1L, 1L)).willReturn(true);

        String token = jwtTokenProvider.generateToken(1L);

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // when - 채팅방 구독
        StompSession.Subscription subscription = session.subscribe("/topic/chat/room/1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // 메시지 수신 처리
            }
        });

        // then
        assertThat(subscription.getSubscriptionId()).isNotNull();
        session.disconnect();
    }
}
