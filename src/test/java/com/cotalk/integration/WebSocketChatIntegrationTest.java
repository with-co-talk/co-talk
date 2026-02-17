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
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import static com.cotalk.integration.WebSocketTestHelper.*;
import static org.awaitility.Awaitility.await;

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
        // Spring Data Redis 설정 (Lettuce)
        // Redisson은 application-test.yml에서 제외됨
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

            // when: A sends message
            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "hi"
            ));

            // then
            Map<String, Object> payload = received.get(15, TimeUnit.SECONDS);
            assertThat(((Number) payload.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) payload.get("senderId")).longValue()).isEqualTo(1L);
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

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

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
            awaitSubscriptionReady(sessionB);

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> e1 = pollChatListNewMessage(chatListEvents, "m1", 15);
            assertThat(e1.get("eventType")).isEqualTo("NEW_MESSAGE");
            assertThat(((Number) e1.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) e1.get("unreadCount")).intValue()).isZero();

            // B 읽음 반영(REST) - 실제 앱과 동일하게 lastReadAt 업데이트
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // 2) B가 구독 해제 후 메시지 전송 -> unreadCount 증가
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

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

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
            awaitSubscriptionReady(sessionB);

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> messagePayload = pollRoomMessage(aRoomEvents, "m1", 15);
            Long messageId = ((Number) messagePayload.get("messageId")).longValue();

            // when: B mark-as-read
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // then: A receives READ room event with lastReadMessageId=messageId
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

        StompSession sessionA = connectWithToken(port, jwtTokenProvider.generateToken(1L));
        StompSession sessionB = connectWithToken(port, jwtTokenProvider.generateToken(2L));

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
            awaitSubscriptionReady(sessionB);

            sessionA.send("/app/chat/message", Map.of(
                    "senderId", 1L,
                    "roomId", roomId,
                    "content", "m1"
            ));

            // 메시지가 브로드캐스트되고 DB에 저장될 때까지 대기
            // (메시지 ID를 얻기 위해 REST API로 조회하거나, 큐에서 메시지 수신을 기다림)
            // 현재는 메시지가 처리될 시간을 주기 위해 최소 대기
            await()
                    .atMost(500, TimeUnit.MILLISECONDS)
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        // 메시지가 처리되었는지 확인: aUserEvents나 bUserEvents에 메시지가 들어왔는지 확인
                        // 또는 REST API로 메시지 조회 시도
                        return !aUserEvents.isEmpty() || !bUserEvents.isEmpty();
                    });

            // when: B mark-as-read
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // then: A receives read-receipt event (B가 읽었음을 알림)
            Map<String, Object> aReadReceipt = pollUserReadReceipt(aUserEvents, 2L, roomId, 15);
            assertThat(((Number) aReadReceipt.get("chatRoomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) aReadReceipt.get("userId")).longValue()).isEqualTo(2L);
            assertThat(aReadReceipt.get("lastReadMessageId")).isNotNull();

            // then: B also receives read-receipt event (자신이 읽었음을 확인)
            Map<String, Object> bReadReceipt = pollUserReadReceipt(bUserEvents, 2L, roomId, 15);
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

}

