package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 *
 * @param secret               JWT 서명에 사용되는 비밀키
 * @param expiration           액세스 토큰 만료 시간 (밀리초)
 * @param refreshToken         리프레시 토큰 설정
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expiration,
        RefreshToken refreshToken
) {
    /**
     * 리프레시 토큰 설정.
     *
     * @param expirationDays 리프레시 토큰 만료 일수
     */
    public record RefreshToken(long expirationDays) {
        public RefreshToken {
            if (expirationDays <= 0) {
                expirationDays = 7;
            }
        }
    }

    public JwtProperties {
        if (expiration <= 0) {
            expiration = 86400000; // 24시간
        }
        if (refreshToken == null) {
            refreshToken = new RefreshToken(7);
        }
    }
}
