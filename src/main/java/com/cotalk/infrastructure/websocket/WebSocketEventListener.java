package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결/해제 이벤트 리스너.
 * WebSocket 세션 연결 및 해제 시 사용자의 온라인 상태를 업데이트한다.
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

    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    /**
     * WebSocket 연결 이벤트를 처리한다.
     * 사용자를 온라인 상태로 변경한다.
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
                log.info("User connected via WebSocket: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in WebSocket connection: {}", userIdStr);
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

        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                updateUserOnlineStatusUseCase.setOffline(userId);
                log.info("User disconnected from WebSocket: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in WebSocket disconnection: {}", userIdStr);
            }
        }
    }
}
