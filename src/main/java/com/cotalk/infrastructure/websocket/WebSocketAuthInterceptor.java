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
import java.util.Optional;

/**
 * WebSocket STOMP 연결 시 JWT 토큰을 검증하는 인터셉터.
 * CONNECT 명령 시 Authorization 헤더에서 JWT 토큰을 추출하여 인증한다.
 *
 * <p>인증 흐름:
 * <ol>
 *   <li>STOMP CONNECT 명령 감지</li>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
 *   <li>JWT 토큰 유효성 검증</li>
 *   <li>사용자 ID를 Principal로 설정</li>
 * </ol>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 메시지 전송 전에 인증을 수행한다.
     * STOMP CONNECT 명령인 경우 JWT 토큰을 검증하고 Principal을 설정한다.
     *
     * @param message 전송할 메시지
     * @param channel 메시지 채널
     * @return 처리된 메시지
     * @throws IllegalArgumentException 인증 토큰이 없거나 유효하지 않은 경우
     */
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

    /**
     * WebSocket 연결 시 인증을 수행한다.
     * 토큰을 추출하고 검증한 후 Principal을 설정한다.
     *
     * @param accessor STOMP 헤더 접근자
     * @throws IllegalArgumentException 인증 토큰이 없거나 유효하지 않은 경우
     */
    private void authenticateConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor)
                .filter(t -> !t.isBlank())
                .orElseThrow(() -> {
                    log.warn("WebSocket connection attempt without token");
                    return new IllegalArgumentException("인증 토큰이 필요합니다.");
                });

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket connection attempt with invalid token");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(new StompPrincipal(userId.toString()));

        log.info("WebSocket connection authenticated for user: {}", userId);
    }

    /**
     * STOMP 헤더에서 JWT 토큰을 추출한다.
     *
     * @param accessor STOMP 헤더 접근자
     * @return 추출된 JWT 토큰을 담은 Optional, 토큰이 없으면 빈 Optional
     */
    private Optional<String> extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);

        if (authHeaders == null || authHeaders.isEmpty()) {
            return Optional.empty();
        }

        String authHeader = authHeaders.get(0);

        if (authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }

        return Optional.of(authHeader);
    }

    /**
     * STOMP 세션에 설정되는 Principal 구현체.
     * 사용자 ID를 이름으로 저장한다.
     *
     * @param name 사용자 ID 문자열
     */
    private record StompPrincipal(String name) implements Principal {
        /**
         * Principal 이름(사용자 ID)을 반환한다.
         *
         * @return 사용자 ID 문자열
         */
        @Override
        public String getName() {
            return name;
        }
    }
}
