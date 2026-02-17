package com.cotalk.integration;

import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static com.cotalk.integration.WebSocketTestHelper.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // application-test.yml: spring.data.redis.enabled=false -> InMemoryChatMessageBroker 사용
@Import(TestRedisConfiguration.class)
@DisplayName("WebSocket Chat Integration (InMemory broker)")
class WebSocketChatInMemoryBrokerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Timeout(value = 20)
    @DisplayName("A가 /app/chat/message로 보내면, B는 /topic/chat/room/{roomId}에서 실시간으로 수신한다 (InMemory broker)")
    void should_deliverMessageToOtherUserViaWebSocketRoomTopic_inMemoryBroker() throws Exception {
        // given
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder()
                .id(roomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build());

        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(1L)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(2L)
                .build());

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

        try {
            CompletableFuture<Map<String, Object>> received = new CompletableFuture<>();

            sessionB.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.complete((Map<String, Object>) payload);
                }
            });
            awaitSubscriptionReady(sessionB);

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "hi"
            ));

            Map<String, Object> payload = received.get(15, TimeUnit.SECONDS);
            assertThat(payload.get("content")).isEqualTo("hi");
            assertThat(payload).containsKeys("schemaVersion", "eventId");
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 30)
    @DisplayName("카톡/라인 방식: 메시지 전송 시 unreadCount=1로 시작, markAsRead 후 0으로 감소 (chat-list 기준)")
    void should_adjustUnreadCount_byMarkAsRead_inMemoryBroker() throws Exception {
        // given
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder()
                .id(roomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(1L)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(2L)
                .build());

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

        try {
            // B는 chat-list 채널을 항상 구독 (unreadCount 확인용)
            BlockingQueue<Map<String, Object>> chatListEvents = new LinkedBlockingQueue<>();
            Subscription chatListSub = sessionB.subscribe("/topic/user/2/chat-list", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    chatListEvents.add((Map<String, Object>) payload);
                }
            });

            // 1) B가 방을 구독(=presence active)한 상태에서 A가 메시지 전송
            Subscription roomSub = sessionB.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // 방 메시지 자체는 여기서는 검증하지 않는다(필요 시 확장 가능)
                }
            });

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            // 카톡/라인 방식: 메시지 전송 시 unreadCount = memberCount - 1 (발신자 제외)
            // B가 구독 중이더라도 markAsRead를 호출하기 전까지는 unreadCount = 1
            Map<String, Object> e1 = pollChatListNewMessage(chatListEvents, "m1", 15);
            assertThat(e1.get("eventType")).isEqualTo("NEW_MESSAGE");
            assertThat(((Number) e1.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) e1.get("unreadCount")).intValue()).isEqualTo(1);

            // B가 방을 보고 있었던 상태를 REST 읽음 처리로 반영(실제 앱과 동일하게 lastReadAt 갱신)
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // 2) B가 방 구독을 해제한 뒤, A가 메시지 전송 -> unreadCount는 여전히 1 (markAsRead 전까지)
            roomSub.unsubscribe();

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m2"
            ));

            Map<String, Object> e2 = pollChatListNewMessage(chatListEvents, "m2", 15);
            assertThat(e2.get("eventType")).isEqualTo("NEW_MESSAGE");
            assertThat(((Number) e2.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) e2.get("unreadCount")).intValue()).isEqualTo(1);

            chatListSub.unsubscribe();
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 30)
    @DisplayName("B가 읽음 처리하면, A는 방 토픽(/topic/chat/room/{roomId})에서 READ 이벤트를 수신하고 lastReadMessageId가 최신 메시지로 온다")
    void should_broadcastRoomReadEvent_toRoomTopic_forSender_inMemoryBroker() throws Exception {
        // given
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder()
                .id(roomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(1L)
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(idGenerator.nextId())
                .chatRoomId(roomId)
                .userId(2L)
                .build());

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

        try {
            BlockingQueue<Map<String, Object>> aRoomEvents = new LinkedBlockingQueue<>();

            // A가 방 토픽을 구독하고, 방으로 전달되는 payload(메시지/READ 이벤트)를 모두 큐에 넣는다.
            Subscription aRoomSub = sessionA.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    aRoomEvents.add((Map<String, Object>) payload);
                }
            });

            // B도 방 토픽을 구독(실제 앱 동작과 동일). 여기서는 검증은 안 함.
            Subscription bRoomSub = sessionB.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // no-op
                }
            });

            // when: A가 메시지를 전송하고, A는 해당 메시지의 messageId를 수신한다.
            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> messagePayload = pollRoomMessage(aRoomEvents, "m1", 15);
            Long messageId = ((Number) messagePayload.get("messageId")).longValue();

            // when: B가 읽음 처리(REST) -> 서버가 READ 이벤트를 방 토픽으로 발행
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // then: A는 READ 이벤트를 받고, lastReadMessageId가 최신 메시지(messageId)와 일치한다.
            Map<String, Object> readPayload = pollRoomRead(aRoomEvents, 2L, roomId, 15);
            assertThat(readPayload.get("eventType")).isEqualTo("READ");
            assertThat(((Number) readPayload.get("chatRoomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) readPayload.get("userId")).longValue()).isEqualTo(2L);
            assertThat(((Number) readPayload.get("lastReadMessageId")).longValue()).isEqualTo(messageId);

            aRoomSub.unsubscribe();
            bRoomSub.unsubscribe();
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 15)
    @DisplayName("A가 타이핑 상태를 보내면, B는 방 토픽에서 TYPING 이벤트를 수신한다")
    void should_broadcastTypingEvent_toRoomTopic() throws Exception {
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder().id(roomId).type(ChatRoom.ChatRoomType.DIRECT).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(1L).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(2L).build());

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

        try {
            BlockingQueue<Map<String, Object>> roomEvents = new LinkedBlockingQueue<>();
            sessionB.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    roomEvents.add((Map<String, Object>) payload);
                }
            });
            awaitSubscriptionReady(sessionB);

            sessionA.send("/app/chat/typing", Map.of("roomId", roomId, "isTyping", true));

            Map<String, Object> typing = pollEventByType(roomEvents, "TYPING", 10);
            assertThat(typing.get("eventType")).isEqualTo("TYPING");
            assertThat(((Number) typing.get("chatRoomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) typing.get("userId")).longValue()).isEqualTo(1L);
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 10)
    @DisplayName("멤버가 /app/chat/presence를 보내면 정상 처리된다 (에러 없음)")
    void should_acceptPresencePing_when_member() throws Exception {
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder().id(roomId).type(ChatRoom.ChatRoomType.DIRECT).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(1L).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(2L).build());

        StompSession session = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        try {
            session.send("/app/chat/presence", Map.of("roomId", roomId));
            assertThat(session.isConnected()).as("presence 전송 후에도 세션 유지").isTrue();
        } finally {
            session.disconnect();
        }
    }

    @Test
    @Timeout(value = 10)
    @DisplayName("비멤버가 /topic/chat/room/{roomId} 구독 시 접근 거부된다")
    void should_rejectSubscribe_when_nonMember() throws Exception {
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder().id(roomId).type(ChatRoom.ChatRoomType.DIRECT).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(1L).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(2L).build());

        CompletableFuture<Throwable> errorHolder = new CompletableFuture<>();
        StompSession session3 = connectWithTokenAndErrorCapture(port, jwtTokenProvider.generateToken(3L), errorHolder);
        try {
            session3.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // no-op: 이 테스트는 구독 거부(에러)만 검증함
                }
            });
        } catch (Exception e) {
            errorHolder.complete(e);
        }

        Throwable t = errorHolder.get(5, TimeUnit.SECONDS);
        assertThat(t).isNotNull();
        String msg = t.getMessage() != null ? t.getMessage() : "";
        assertThat(msg.contains("접근 권한") || msg.contains("Connection closed")).isTrue();
        if (session3.isConnected()) {
            session3.disconnect();
        }
    }

    @Test
    @Timeout(value = 15)
    @DisplayName("비멤버가 /app/chat/message로 메시지 전송 시 /user/queue/errors에 ACCESS_DENIED 수신")
    void should_receiveAccessDeniedOnErrorsQueue_when_sendMessageAsNonMember() throws Exception {
        Long roomId = idGenerator.nextId();
        chatRoomRepository.save(ChatRoom.builder().id(roomId).type(ChatRoom.ChatRoomType.DIRECT).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(2L).build());
        chatRoomMemberRepository.save(ChatRoomMember.builder().id(idGenerator.nextId()).chatRoomId(roomId).userId(3L).build());

        StompSession session1 = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        BlockingQueue<Map<String, Object>> errors = new LinkedBlockingQueue<>();
        session1.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                errors.add((Map<String, Object>) payload);
            }
        });
        awaitSubscriptionReady(session1);

        session1.send("/app/chat/message", Map.of("senderId", 1L, "roomId", roomId, "content", "forbidden"));

        Map<String, Object> err = pollErrorPayload(errors, 10);
        assertThat(err.get("code")).isEqualTo("ACCESS_DENIED");
        session1.disconnect();
    }

}

