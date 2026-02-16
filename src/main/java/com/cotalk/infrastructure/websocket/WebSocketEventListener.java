package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.infrastructure.metrics.CustomMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 연결/해제 이벤트 리스너.
 * WebSocket 세션 연결 및 해제 시 사용자의 온라인 상태를 업데이트한다.
 *
 * <p>멀티 인스턴스 환경을 지원하기 위해 세션 추적을 Redis Set으로 관리한다.
 * Redis 키: {@code ws:user:{userId}:sessions} (TTL 2시간)
 *
 * <p>처리하는 이벤트:
 * <ul>
 *   <li>{@link SessionConnectedEvent} - 연결 시 온라인 상태로 변경</li>
 *   <li>{@link SessionDisconnectEvent} - 해제 시 오프라인 상태로 변경</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final String SESSION_KEY_PREFIX = "ws:user:";
    private static final String SESSION_KEY_SUFFIX = ":sessions";
    private static final long SESSION_TTL_SECONDS = 7200;

    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;
    private final ChatRoomPresenceTracker chatRoomPresenceTracker;
    private final CustomMetrics customMetrics;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * subscriptionId -> roomId 매핑(세션별).
     * <p>UNSUBSCRIBE 이벤트에서 destination을 직접 얻기 어려워 subscriptionId로 추적한다.
     * 로컬 인메모리 맵으로 유지한다 (SUBSCRIBE/UNSUBSCRIBE는 같은 연결에서 발생).</p>
     */
    private final Map<String, Map<String, Long>> roomsBySessionAndSubscription = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 이벤트를 처리한다.
     * 사용자를 온라인 상태로 변경한다.
     * 사용자가 DB에 존재하지 않는 경우(삭제됨 등) 예외를 무시하고 로깅만 수행한다.
     *
     * @param event WebSocket 연결 이벤트
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                updateUserOnlineStatusUseCase.setOnline(userId);
                customMetrics.incrementWebSocketConnections();
                String sessionId = headerAccessor.getSessionId();
                if (sessionId != null) {
                    String sessionKey = sessionKey(userId);
                    redisTemplate.opsForSet().add(sessionKey, sessionId);
                    redisTemplate.expire(sessionKey, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
                }
                log.info("User connected via WebSocket: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in WebSocket connection: {}", userIdStr);
            } catch (Exception e) {
                // UserNotFoundException 등 - 토큰은 유효하지만 사용자가 DB에 없는 경우
                // 클라이언트는 REST API 호출 시 401/404를 받아 로그아웃 처리됨
                log.warn("Failed to set user online status: userId={}, error={}", userIdStr, e.getMessage());
            }
        }
    }

    /**
     * WebSocket 해제 이벤트를 처리한다.
     * 사용자를 오프라인 상태로 변경한다.
     *
     * @param event WebSocket 해제 이벤트
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
        String sessionId = headerAccessor.getSessionId();

        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);

                // 멀티 인스턴스 세션 지원: Redis Set에서 세션 제거 후 남은 세션이 없을 때만 오프라인 처리
                customMetrics.decrementWebSocketConnections();
                String sessionKey = sessionKey(userId);
                redisTemplate.opsForSet().remove(sessionKey, sessionId);
                Long remaining = redisTemplate.opsForSet().size(sessionKey);
                if (remaining == null || remaining == 0) {
                    redisTemplate.delete(sessionKey);
                    updateUserOnlineStatusUseCase.setOffline(userId);
                    log.info("User disconnected from WebSocket (last session): userId={}", userId);
                } else {
                    log.info("User disconnected from WebSocket (remaining sessions: {}): userId={}", remaining, userId);
                }

                // presence 정리
                clearPresenceForSession(userId, sessionId);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in WebSocket disconnection: {}", userIdStr);
            }
        }
    }

    /**
     * STOMP 구독 이벤트를 처리한다.
     * 사용자가 /topic/chat/room/{roomId}를 구독하면 해당 방을 "활성"으로 기록한다.
     */
    @EventListener
    public void handleWebSocketSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String subscriptionId = headerAccessor.getSubscriptionId();
        String sessionId = headerAccessor.getSessionId();
        String userIdStr = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (destination == null || subscriptionId == null || sessionId == null || userIdStr == null) {
            return;
        }

        Long roomId = parseRoomIdFromRoomTopic(destination);
        if (roomId == null) {
            return;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            chatRoomPresenceTracker.markActive(roomId, userId, sessionId);
            log.info(
                    "[WS] SUBSCRIBE sessionId={}, subscriptionId={}, userId={}, destination={}, roomId={}",
                    sessionId, subscriptionId, userId, destination, roomId
            );
            roomsBySessionAndSubscription
                    .computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>())
                    .put(subscriptionId, roomId);
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID in WebSocket subscribe: {}", userIdStr);
        }
    }

    /**
     * STOMP 구독 해제 이벤트를 처리한다.
     * subscriptionId로 매핑된 roomId를 찾아 비활성 처리한다.
     */
    @EventListener
    public void handleWebSocketUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscriptionId = headerAccessor.getSubscriptionId();
        String sessionId = headerAccessor.getSessionId();
        String userIdStr = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (subscriptionId == null || sessionId == null || userIdStr == null) {
            return;
        }

        Map<String, Long> subscriptionMap = roomsBySessionAndSubscription.get(sessionId);
        if (subscriptionMap == null) {
            return;
        }
        Long roomId = subscriptionMap.remove(subscriptionId);
        if (roomId == null) {
            return;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            chatRoomPresenceTracker.markInactive(roomId, userId, sessionId);
            log.info(
                    "[WS] UNSUBSCRIBE sessionId={}, subscriptionId={}, userId={}, roomId={}",
                    sessionId, subscriptionId, userId, roomId
            );
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID in WebSocket unsubscribe: {}", userIdStr);
        }
    }

    private void clearPresenceForSession(Long userId, String sessionId) {
        if (sessionId == null) {
            chatRoomPresenceTracker.clearSession(userId, null);
            return;
        }
        Map<String, Long> roomsBySubscription = roomsBySessionAndSubscription.remove(sessionId);
        if (roomsBySubscription == null) {
            chatRoomPresenceTracker.clearSession(userId, sessionId);
            return;
        }
        for (Long roomId : roomsBySubscription.values()) {
            chatRoomPresenceTracker.markInactive(roomId, userId, sessionId);
            log.info("[WS] DISCONNECT cleanup sessionId={}, userId={}, roomId={}", sessionId, userId, roomId);
        }
        chatRoomPresenceTracker.clearSession(userId, sessionId);
    }

    /**
     * Redis에서 사용자 세션 Set의 키를 생성한다.
     *
     * @param userId 사용자 ID
     * @return Redis 키 (예: {@code ws:user:123:sessions})
     */
    private String sessionKey(Long userId) {
        return SESSION_KEY_PREFIX + userId + SESSION_KEY_SUFFIX;
    }

    /**
     * /topic/chat/room/{roomId}만 "활성 방"으로 인정한다.
     * (/reaction 등 suffix는 제외)
     */
    private Long parseRoomIdFromRoomTopic(String destination) {
        String prefix = "/topic/chat/room/";
        if (!destination.startsWith(prefix)) {
            return null;
        }
        String rest = destination.substring(prefix.length());
        // reaction 등 suffix 제외: 숫자만 허용
        if (!rest.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(rest);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
