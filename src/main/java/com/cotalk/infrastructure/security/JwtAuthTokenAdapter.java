package com.cotalk.infrastructure.security;

import com.cotalk.domain.port.outbound.AuthTokenPort;
import org.springframework.stereotype.Component;

/**
 * AuthTokenPort의 JWT 구현 어댑터.
 * Access 토큰 발급을 JwtTokenProvider에 위임한다.
 */
@Component
public class JwtAuthTokenAdapter implements AuthTokenPort {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthTokenAdapter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String generateAccessToken(Long userId) {
        return jwtTokenProvider.generateToken(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAccessTokenExpiresInSeconds() {
        return jwtTokenProvider.getExpiresInSeconds();
    }
}
