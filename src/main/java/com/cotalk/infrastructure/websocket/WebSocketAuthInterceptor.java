package com.cotalk.infrastructure.websocket;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket STOMP 연결 시 JWT 토큰을 검증하는 인터셉터.
 * CONNECT 명령 시 Authorization 헤더에서 JWT 토큰을 추출하여 인증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // CONNECT 명령일 때만 인증 검사
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token == null || token.isBlank()) {
            log.warn("WebSocket connection attempt without token");
            throw new IllegalArgumentException("인증 토큰이 필요합니다.");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket connection attempt with invalid token");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(new StompPrincipal(userId.toString()));
        
        log.info("WebSocket connection authenticated for user: {}", userId);
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);
        
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.get(0);
        
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return authHeader;
    }

    /**
     * STOMP 세션에 설정되는 Principal 구현체
     */
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
