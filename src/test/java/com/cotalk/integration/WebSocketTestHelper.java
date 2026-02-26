package com.cotalk.integration;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * WebSocket 통합 테스트 공통 헬퍼 클래스.
 * WebSocket 연결, 구독 대기, 메시지 폴링 등의 공통 로직을 제공한다.
 *
 * @author seunggu.lee
 */
public final class WebSocketTestHelper {

    private WebSocketTestHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * JWT 토큰으로 WebSocket STOMP 세션을 연결한다.
     *
     * @param port 서버 포트
     * @param token JWT 토큰
     * @return 연결된 STOMP 세션
     * @throws Exception 연결 실패 시
     */
    public static StompSession connectWithToken(int port, String token) throws Exception {
        return connectWithTokenAndErrorCapture(port, token, null);
    }

    /**
     * JWT 토큰으로 WebSocket STOMP 세션을 연결하고, 에러를 캡처할 수 있도록 한다.
     *
     * @param port 서버 포트
     * @param token JWT 토큰
     * @param errorHolder 에러를 담을 CompletableFuture (null이면 에러 캡처 안 함)
     * @return 연결된 STOMP 세션
     * @throws Exception 연결 실패 시
     */
    public static StompSession connectWithTokenAndErrorCapture(
            int port, String token, CompletableFuture<Throwable> errorHolder) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                       byte[] payload, Throwable exception) {
                if (errorHolder != null) {
                    errorHolder.complete(exception);
                }
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                if (errorHolder != null && !errorHolder.isDone()) {
                    errorHolder.complete(exception);
                }
            }
        };

        return stompClient.connectAsync(
                String.format("ws://localhost:%d/ws", port),
                new WebSocketHttpHeaders(),
                connectHeaders,
                handler
        ).get(10, TimeUnit.SECONDS);
    }

    /**
     * 구독이 브로커에 반영될 때까지 대기한다.
     * Awaitility를 사용하여 세션이 연결된 상태가 유지되는지 짧게 대기한다.
     *
     * @param session STOMP 세션
     */
    public static void awaitSubscriptionReady(StompSession session) {
        if (!session.isConnected()) {
            return;
        }
        await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(session::isConnected);
    }

    /**
     * STOMP 구독이 SimpleBroker에 실제로 등록될 때까지 대기한다.
     *
     * <p>서버 측 {@link SimpMessagingTemplate}으로 probe 메시지를 전송하고,
     * 구독자가 해당 메시지를 수신하면 구독이 완료된 것으로 간주한다.
     * 이 방식은 STOMP SUBSCRIBE 프레임이 서버의 인바운드 채널에서 비동기로 처리되는
     * 타이밍 문제를 확실하게 해결한다.</p>
     *
     * <p>probe 메시지는 {@code _probe=true} 키를 포함하며,
     * 기존 poll 메서드들({@code pollRoomMessage}, {@code pollChatListNewMessage} 등)은
     * 이 키를 가진 메시지를 자동으로 무시한다.</p>
     *
     * @param template    서버 측 메시지 전송 템플릿
     * @param destination 구독 확인할 STOMP destination
     * @param queue       해당 구독의 메시지를 수신하는 큐
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static void awaitSubscriptionReady(SimpMessagingTemplate template,
                                              String destination,
                                              BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            // 서버 측에서 직접 메시지 전송 → SimpleBroker가 구독자에게 전달
            template.convertAndSend(destination, Map.of("_probe", true));

            // probe가 도착했는지 확인 (짧은 타임아웃으로 폴링)
            Map<String, Object> received = queue.poll(300, TimeUnit.MILLISECONDS);
            if (received != null) {
                if (Boolean.TRUE.equals(received.get("_probe"))) {
                    // 구독 확인 완료. 잔여 probe 메시지 제거
                    queue.removeIf(m -> Boolean.TRUE.equals(m.get("_probe")));
                    return;
                }
                // probe가 아닌 실제 메시지가 먼저 도착한 경우 → 큐에 복원
                queue.add(received);
            }
        }
        throw new AssertionError("STOMP 구독이 10초 내에 등록되지 않음: " + destination);
    }

    /**
     * 채팅방 메시지를 큐에서 폴링한다.
     *
     * @param queue 메시지 큐
     * @param expectedContent 예상 메시지 내용
     * @param timeoutSeconds 타임아웃 (초)
     * @return 메시지 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollRoomMessage(
            BlockingQueue<Map<String, Object>> queue,
            String expectedContent,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
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

    /**
     * READ 이벤트를 큐에서 폴링한다.
     *
     * @param queue 이벤트 큐
     * @param expectedReaderId 예상 읽은 사용자 ID
     * @param expectedRoomId 예상 채팅방 ID
     * @param timeoutSeconds 타임아웃 (초)
     * @return READ 이벤트 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollRoomRead(
            BlockingQueue<Map<String, Object>> queue,
            Long expectedReaderId,
            Long expectedRoomId,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
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

    /**
     * chat-list NEW_MESSAGE 이벤트를 큐에서 폴링한다.
     *
     * @param queue 이벤트 큐
     * @param expectedLastMessage 예상 마지막 메시지 내용
     * @param timeoutSeconds 타임아웃 (초)
     * @return chat-list 이벤트 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollChatListNewMessage(
            BlockingQueue<Map<String, Object>> queue,
            String expectedLastMessage,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
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

    /**
     * 사용자 read-receipt 이벤트를 큐에서 폴링한다.
     *
     * @param queue 이벤트 큐
     * @param expectedReaderId 예상 읽은 사용자 ID
     * @param expectedRoomId 예상 채팅방 ID
     * @param timeoutSeconds 타임아웃 (초)
     * @return read-receipt 이벤트 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollUserReadReceipt(
            BlockingQueue<Map<String, Object>> queue,
            Long expectedReaderId,
            Long expectedRoomId,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
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

    /**
     * 특정 eventType의 이벤트를 큐에서 폴링한다.
     *
     * @param queue 이벤트 큐
     * @param eventType 예상 이벤트 타입
     * @param timeoutSeconds 타임아웃 (초)
     * @return 이벤트 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollEventByType(
            BlockingQueue<Map<String, Object>> queue,
            String eventType,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e == null) continue;
            if (eventType.equals(e.get("eventType"))) return e;
        }
        throw new AssertionError("Event type " + eventType + " not received");
    }

    /**
     * 에러 payload를 큐에서 폴링한다.
     *
     * @param queue 에러 큐
     * @param timeoutSeconds 타임아웃 (초)
     * @return 에러 payload
     * @throws InterruptedException 인터럽트 발생 시
     */
    public static Map<String, Object> pollErrorPayload(
            BlockingQueue<Map<String, Object>> queue,
            int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> e = queue.poll(1, TimeUnit.SECONDS);
            if (e != null && e.get("code") != null) return e;
        }
        throw new AssertionError("Error payload not received");
    }

    /**
     * REST API를 통해 채팅방 읽음 처리를 수행한다.
     *
     * @param restTemplate TestRestTemplate
     * @param jwtTokenProvider JWT 토큰 제공자
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    public static void markAsReadViaRest(
            TestRestTemplate restTemplate,
            JwtTokenProvider jwtTokenProvider,
            Long userId,
            Long roomId) {
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
}
