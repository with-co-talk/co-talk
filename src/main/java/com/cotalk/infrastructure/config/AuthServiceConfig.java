package com.cotalk.infrastructure.config;

import com.cotalk.application.service.auth.RefreshTokenService;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.infrastructure.config.properties.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인증 관련 Use Case 빈 설정.
 * 인프라 설정(JwtProperties 등)을 애플리케이션 서비스에 주입할 때 사용한다.
 */
@Configuration
public class AuthServiceConfig {

    /**
     * RefreshTokenService는 리프레시 토큰 만료 일수를 인프라 설정에서 받으므로
     * 여기서 빈으로 생성한다.
     */
    @Bean
    public RefreshTokenService refreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthTokenPort authTokenPort,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            JwtProperties jwtProperties) {
        long expirationDays = jwtProperties.refreshToken().expirationDays();
        return new RefreshTokenService(
                refreshTokenRepository,
                authTokenPort,
                idGenerator,
                timeProvider,
                expirationDays);
    }
}
