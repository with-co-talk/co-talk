package com.cotalk.infrastructure.websocket;

import com.cotalk.infrastructure.config.properties.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 메시지 브로커 설정 클래스.
 * STOMP 프로토콜을 사용한 실시간 메시징을 구성한다.
 *
 * <p>메시지 브로커 설정:
 * <ul>
 *   <li>/topic, /queue - 클라이언트 구독 prefix</li>
 *   <li>/app - 서버로 메시지 전송 prefix</li>
 *   <li>/user - 특정 사용자 메시지 prefix</li>
 * </ul>
 *
 * <p>WebSocket 엔드포인트:
 * <ul>
 *   <li>/ws - SockJS 지원 엔드포인트</li>
 *   <li>/ws - 순수 WebSocket 엔드포인트</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            AppProperties appProperties) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.allowedOrigins = appProperties.cors().allowedOrigins().split(",");
    }

    /**
     * 메시지 브로커를 구성한다.
     * 구독 경로, 애플리케이션 목적지 prefix, 사용자 목적지 prefix를 설정한다.
     *
     * @param config 메시지 브로커 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 설정 - 클라이언트가 구독할 prefix
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix
        config.setApplicationDestinationPrefixes("/app");
        // 특정 사용자에게 메시지를 보낼 때 사용하는 prefix
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트를 등록한다.
     * SockJS 폴백을 지원하는 엔드포인트와 순수 WebSocket 엔드포인트를 모두 등록한다.
     *
     * @param registry STOMP 엔드포인트 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();

        // SockJS 없이 순수 WebSocket 연결
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);
    }

    /**
     * 클라이언트 인바운드 채널을 구성한다.
     * WebSocket 인증 인터셉터를 등록하여 연결 시 JWT 인증을 수행한다.
     *
     * @param registration 채널 등록 객체
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // WebSocket 인증 인터셉터 등록
        registration.interceptors(webSocketAuthInterceptor);
    }
}
