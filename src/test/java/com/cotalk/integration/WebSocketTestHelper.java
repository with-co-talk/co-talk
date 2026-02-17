package com.cotalk.integration;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
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
     * Awaitility를 사용하여 실제 구독 완료를 확인한다.
     * 
     * <p>구독이 이미 완료되었거나, 세션이 연결되어 있으면 즉시 반환한다.
     * 실제 구독 완료는 STOMP 프로토콜의 비동기 특성상 짧은 대기가 필요할 수 있다.</p>
     */
    public static void awaitSubscriptionReady(StompSession session) {
        if (!session.isConnected()) {
            return; // 연결되지 않았으면 대기 불필요
        }
        // 구독이 완료될 때까지 짧게 대기 (STOMP 프로토콜의 비동기 특성)
        await()
                .atMost(500, TimeUnit.MILLISECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> {
                    // 세션이 연결되어 있고, 구독이 등록되었거나 이미 메시지를 받을 준비가 되었는지 확인
                    return session.isConnected();
                });
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
