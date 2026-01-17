package com.cotalk.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 제공자.
 * JWT 토큰의 생성, 검증, 파싱 기능을 제공한다.
 *
 * <p>설정 프로퍼티:
 * <ul>
 *   <li>{@code jwt.secret} - JWT 서명에 사용할 비밀키</li>
 *   <li>{@code jwt.expiration} - 토큰 만료 시간 (밀리초)</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * JwtTokenProvider 생성자.
     *
     * @param secret JWT 서명에 사용할 비밀키 문자열
     * @param expiration 토큰 만료 시간 (밀리초)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 사용자 ID를 기반으로 JWT 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출한다.
     *
     * @param token JWT 토큰 문자열
     * @return 토큰에 포함된 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * JWT 토큰의 유효성을 검증한다.
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JWT 토큰의 만료 여부를 확인한다.
     *
     * @param token 확인할 JWT 토큰 문자열
     * @return 토큰이 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
