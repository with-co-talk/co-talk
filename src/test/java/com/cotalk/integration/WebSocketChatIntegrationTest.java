package com.cotalk.integration;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static com.cotalk.integration.WebSocketTestHelper.*;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // 프로덕션 SecurityFilterChain 비활성화 → IntegrationTestSecurityConfig만 사용
                "app.security.default-chain.enabled=false",
                "spring.data.redis.enabled=true",
                // application-test.yml의 RedisAutoConfiguration exclude를 재정의하여 Redis 자동 설정 허용
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
        }
)
@ActiveProfiles("test")
@Import(IntegrationTestSecurityConfig.class)
@DisplayName("WebSocket Chat Integration")
class WebSocketChatIntegrationTest {

    @SuppressWarnings("resource")
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

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Redis Pub/Sub 리스너가 완전히 초기화될 때까지 대기한다.
     * {@link RedisMessageListenerContainer#start()}는 비동기로 PSUBSCRIBE를 등록하므로,
     * CI 환경에서 테스트 실행 전에 구독이 완료되지 않는 레이스 컨디션이 발생할 수 있다.
     *
     * <p>{@code isListening()}은 내부 상태 머신이 {@code State.listening()}으로 전이된 후에만 true를 반환한다.
     * 이 전이는 PSUBSCRIBE 명령이 Redis 서버에서 성공적으로 처리된 후 발생하므로,
     * 서버 측 구독 등록을 신뢰할 수 있게 보장한다.</p>
     */
    @BeforeEach
    void waitForRedisPubSubReady() {
        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(redisMessageListenerContainer::isListening);
    }

    @Test
    @Timeout(value = 30)
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
            BlockingQueue<Map<String, Object>> bRoomEvents = new LinkedBlockingQueue<>();

            sessionB.subscribe("/topic/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @SuppressWarnings("unchecked")
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    bRoomEvents.add((Map<String, Object>) payload);
                }
            });

            // probe 기반 구독 확인: SimpleBroker에 실제 등록될 때까지 대기
            awaitSubscriptionReady(messagingTemplate, "/topic/chat/room/" + roomId, bRoomEvents);

            // when: A sends message
            sessionA.send("/app/chat/message", Map.of(
                    "roomId", roomId,
                    "content", "hi"
            ));

            // then
            Map<String, Object> payload = pollRoomMessage(bRoomEvents, "hi", 15);
            assertThat(((Number) payload.get("roomId")).longValue()).isEqualTo(roomId);
            assertThat(((Number) payload.get("senderId")).longValue()).isEqualTo(1L);
            assertThat(payload).containsEntry("content", "hi");
            assertThat(payload).containsKeys("schemaVersion", "eventId");
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

    @Test
    @Timeout(value = 30)
    @DisplayName("메시지 전송 시 unreadCount=1로 시작, markAsRead 후 다시 전송하면 unreadCount=1 (Redis Pub/Sub + chat-list)")
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

            // B가 방 토픽 구독 (presence 목적)
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

            // chat-list 구독이 SimpleBroker에 등록될 때까지 대기
            awaitSubscriptionReady(messagingTemplate, "/topic/user/2/chat-list", chatListEvents);

            // 1) A가 메시지 전송 → B의 lastReadMessageId가 NULL이므로 unreadCount = 1
            sessionA.send("/app/chat/message", Map.of(
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> e1 = pollChatListNewMessage(chatListEvents, "m1", 15);
            assertThat(e1).containsEntry("eventType", "NEW_MESSAGE");
            assertThat(((Number) e1.get("roomId")).longValue()).isEqualTo(roomId);
            // B가 구독 중이더라도 markAsRead를 호출하기 전까지는 unreadCount = 1
            assertThat(((Number) e1.get("unreadCount")).intValue()).isEqualTo(1);

            // B 읽음 반영(REST) - 실제 앱과 동일하게 lastReadAt 업데이트
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // 2) B가 구독 해제 후 메시지 전송 → markAsRead 이후 새 메시지이므로 unreadCount = 1
            roomSub.unsubscribe();

            sessionA.send("/app/chat/message", Map.of(
                    "roomId", roomId,
                    "content", "m2"
            ));

            Map<String, Object> e2 = pollChatListNewMessage(chatListEvents, "m2", 15);
            assertThat(e2).containsEntry("eventType", "NEW_MESSAGE");
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

            // A의 구독이 SimpleBroker에 등록될 때까지 대기 (A가 메시지를 수신해야 하므로)
            awaitSubscriptionReady(messagingTemplate, "/topic/chat/room/" + roomId, aRoomEvents);

            sessionA.send("/app/chat/message", Map.of(
                    "roomId", roomId,
                    "content", "m1"
            ));

            Map<String, Object> messagePayload = pollRoomMessage(aRoomEvents, "m1", 15);
            Long messageId = ((Number) messagePayload.get("messageId")).longValue();

            // when: B mark-as-read
            markAsReadViaRest(restTemplate, jwtTokenProvider, 2L, roomId);

            // then: A receives READ room event with lastReadMessageId=messageId
            Map<String, Object> readPayload = pollRoomRead(aRoomEvents, 2L, roomId, 15);
            assertThat(readPayload).containsEntry("eventType", "READ");
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
            BlockingQueue<Map<String, Object>> aRoomEvents = new LinkedBlockingQueue<>();

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

            // A가 방 토픽도 구독하여 메시지 전달 확인용으로 사용 (메시지가 DB에 저장 + 브로드캐스트 완료 확인)
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

            // A의 read-receipt 구독과 room 구독이 모두 등록될 때까지 대기
            awaitSubscriptionReady(messagingTemplate, "/topic/user/1/read-receipt", aUserEvents);
            awaitSubscriptionReady(messagingTemplate, "/topic/chat/room/" + roomId, aRoomEvents);
            // B의 read-receipt 구독도 확인
            awaitSubscriptionReady(messagingTemplate, "/topic/user/2/read-receipt", bUserEvents);

            sessionA.send("/app/chat/message", Map.of(
                    "roomId", roomId,
                    "content", "m1"
            ));

            // 메시지가 DB에 저장되고 브로드캐스트될 때까지 대기 (방 토픽에서 메시지 수신으로 확인)
            pollRoomMessage(aRoomEvents, "m1", 15);

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
            assertThat(aReadReceipt).containsEntry("eventId", bReadReceipt.get("eventId"));

            aUserSub.unsubscribe();
            bUserSub.unsubscribe();
            aRoomSub.unsubscribe();
        } finally {
            sessionA.disconnect();
            sessionB.disconnect();
        }
    }

}
