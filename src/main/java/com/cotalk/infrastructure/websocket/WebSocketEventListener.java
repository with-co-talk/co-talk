package com.cotalk.infrastructure.websocket;

import com.cotalk.domain.port.inbound.UpdateUserOnlineStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결/해제 이벤트 리스너
 * 연결 시 온라인 상태로, 해제 시 오프라인 상태로 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

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
