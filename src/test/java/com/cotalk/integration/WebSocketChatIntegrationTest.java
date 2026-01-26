package com.cotalk.integration;

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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.data.redis.enabled=true"
        }
)
@ActiveProfiles("test")
@DisplayName("WebSocket Chat Integration")
class WebSocketChatIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

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
    @DisplayName("A가 /app/chat/message로 보내면, B는 /topic/chat/room/{roomId}에서 실시간으로 수신한다 (Redis Pub/Sub 포함)")
    void should_deliverMessageToOtherUserViaWebSocketRoomTopic() throws Exception {
        // given: room + members
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

        // connect sessions
        StompSession sessionA = connectWithToken(jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(jwtTokenProvider.generateToken(2L));

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

            // when: A sends message
            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "hi"
            ));

            // then
            Map<String, Object> payload = received.get(10, TimeUnit.SECONDS);
            assertThat(payload.get("roomId")).isEqualTo(roomId.intValue());
            assertThat(payload.get("senderId")).isEqualTo(1);
            assertThat(payload.get("content")).isEqualTo("hi");
            assertThat(payload).containsKeys("schemaVersion", "eventId");
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 30)
    @DisplayName("B가 방 구독 중이면 unreadCount=0, 구독 해제하면 unreadCount가 증가한다 (Redis Pub/Sub + chat-list)")
    void should_adjustUnreadCount_byPresenceSubscription_withRedis() throws Exception {
        // given: room + members
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

        StompSession sessionA = connectWithToken(jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(jwtTokenProvider.generateToken(2L));

        try {
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
                    // no-op
                }
            });

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> e1 = pollChatListNewMessage(chatListEvents, "m1");
            assertThat(e1.get("eventType")).isEqualTo("NEW_MESSAGE");
            assertThat(((Number) e1.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) e1.get("unreadCount")).intValue()).isZero();

            // B 읽음 반영(REST) - 실제 앱과 동일하게 lastReadAt 업데이트
            markAsReadViaRest(2L, roomId);

            // 2) B가 구독 해제 후 메시지 전송 -> unreadCount 증가
            roomSub.unsubscribe();

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m2"
            ));

            Map<String, Object> e2 = pollChatListNewMessage(chatListEvents, "m2");
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
    @DisplayName("B가 읽음 처리하면, A는 방 토픽(/topic/chat/room/{roomId})에서 READ 이벤트를 수신한다 (Redis Pub/Sub)")
    void should_broadcastRoomReadEvent_toRoomTopic_forSender_withRedis() throws Exception {
        // given: room + members
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

        StompSession sessionA = connectWithToken(jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(jwtTokenProvider.generateToken(2L));

        try {
            BlockingQueue<Map<String, Object>> aRoomEvents = new LinkedBlockingQueue<>();

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

            // B도 방 토픽 구독(실제 앱 동작과 동일)
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

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> messagePayload = pollRoomMessage(aRoomEvents, "m1");
            Long messageId = ((Number) messagePayload.get("messageId")).longValue();

            // when: B mark-as-read
            markAsReadViaRest(2L, roomId);

            // then: A receives READ room event with lastReadMessageId=messageId
            Map<String, Object> readPayload = pollRoomRead(aRoomEvents, 2L, roomId);
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
    @Timeout(value = 30)
    @DisplayName("B가 읽음 처리하면, A와 B 모두 사용자 채널(/topic/user/{userId}/read-receipt)에서 READ 이벤트를 수신한다")
    void should_broadcastReadReceiptEvent_toUserChannel_forAllMembers() throws Exception {
        // given: room + members
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

        StompSession sessionA = connectWithToken(jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(jwtTokenProvider.generateToken(2L));

        try {
            BlockingQueue<Map<String, Object>> aUserEvents = new LinkedBlockingQueue<>();
            BlockingQueue<Map<String, Object>> bUserEvents = new LinkedBlockingQueue<>();

            // A가 사용자 채널 구독
            Subscription aUserSub = sessionA.subscribe("/topic/user/1/read-receipt", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    aUserEvents.add((Map<String, Object>) payload);
                }
            });

            // B가 사용자 채널 구독
            Subscription bUserSub = sessionB.subscribe("/topic/user/2/read-receipt", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    bUserEvents.add((Map<String, Object>) payload);
                }
            });

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            // 메시지 ID를 얻기 위해 잠시 대기
            Thread.sleep(500);

            // when: B mark-as-read
            markAsReadViaRest(2L, roomId);

            // then: A receives read-receipt event (B가 읽었음을 알림)
            Map<String, Object> aReadReceipt = pollUserReadReceipt(aUserEvents, 2L, roomId);
            assertThat(((Number) aReadReceipt.get("chatRoomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) aReadReceipt.get("userId")).longValue()).isEqualTo(2L);
            assertThat(aReadReceipt.get("lastReadMessageId")).isNotNull();

            // then: B also receives read-receipt event (자신이 읽었음을 확인)
            Map<String, Object> bReadReceipt = pollUserReadReceipt(bUserEvents, 2L, roomId);
            assertThat(((Number) bReadReceipt.get("chatRoomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) bReadReceipt.get("userId")).longValue()).isEqualTo(2L);
            assertThat(bReadReceipt.get("lastReadMessageId")).isNotNull();

            // 이벤트 ID가 동일한지 확인 (중복 체크를 위해)
            assertThat(aReadReceipt.get("eventId")).isEqualTo(bReadReceipt.get("eventId"));

            aUserSub.unsubscribe();
            bUserSub.unsubscribe();
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    private Map<String, Object> pollUserReadReceipt(
            BlockingQueue<Map<String, Object>> queue,
            Long expectedReaderId,
            Long expectedRoomId
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e == null) continue;
            Object roomId = e.get("chatRoomId");
            Object userId = e.get("userId");
            if (roomId == null || userId == null) continue;
            if (((Number) roomId).longValue() == expectedRoomId && ((Number) userId).longValue() == expectedReaderId) {
                return e;
            }
        }
        throw new AssertionError("Read receipt event not received for roomId=" + expectedRoomId + ", readerId=" + expectedReaderId);
    }

    private Map<String, Object> pollRoomMessage(
            BlockingQueue<Map<String, Object>> queue,
            String expectedContent
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e == null) continue;
            Object content = e.get("content");
            Object messageId = e.get("messageId");
            Object eventType = e.get("eventType");
            if (expectedContent.equals(content) && messageId != null && eventType == null) {
                return e;
            }
        }
        throw new AssertionError("Room message not received for content=" + expectedContent);
    }

    private Map<String, Object> pollRoomRead(
            BlockingQueue<Map<String, Object>> queue,
            Long expectedReaderId,
            Long expectedRoomId
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e == null) continue;
            Object eventType = e.get("eventType");
            if (!"READ".equals(eventType)) continue;
            Object roomId = e.get("chatRoomId");
            Object userId = e.get("userId");
            if (roomId == null || userId == null) continue;
            if (((Number) roomId).longValue() == expectedRoomId && ((Number) userId).longValue() == expectedReaderId) {
                return e;
            }
        }
        throw new AssertionError("READ room event not received for roomId=" + expectedRoomId + ", readerId=" + expectedReaderId);
    }

    private Map<String, Object> pollChatListNewMessage(
            BlockingQueue<Map<String, Object>> queue,
            String expectedLastMessage
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e == null) continue;
            Object eventType = e.get("eventType");
            Object lastMessage = e.get("lastMessage");
            if ("NEW_MESSAGE".equals(eventType) && expectedLastMessage.equals(lastMessage)) {
                return e;
            }
        }
        throw new AssertionError("NEW_MESSAGE chat-list event not received for lastMessage=" + expectedLastMessage);
    }

    private void markAsReadViaRest(Long userId, Long roomId) {
        String token = jwtTokenProvider.generateToken(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        ResponseEntity<String> res = restTemplate.exchange(
                "/api/v1/chat/rooms/" + roomId + "/read",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private StompSession connectWithToken(String token) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }
}

