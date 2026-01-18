package com.cotalk.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_KEY_LENGTH = 32; // 256비트

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * JwtTokenProvider 생성자.
     *
     * @param secret JWT 서명에 사용할 비밀키 문자열 (최소 32자 필요)
     * @param expiration 토큰 만료 시간 (밀리초)
     * @throws IllegalArgumentException 시크릿 키가 누락되거나 길이가 부족한 경우
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        validateSecret(secret);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        log.info("JWT 토큰 제공자 초기화 완료 - 만료 시간: {}ms", expiration);
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT 시크릿 키가 설정되지 않았습니다. JWT_SECRET 환경변수를 설정하세요.");
        }
        if (secret.length() < MIN_SECRET_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "JWT 시크릿 키는 최소 " + MIN_SECRET_KEY_LENGTH + "자 이상이어야 합니다. " +
                            "현재 길이: " + secret.length());
        }
    }

    /**
     * 사용자 ID를 기반으로 JWT 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Long userId) {
        return generateToken(userId, "USER");
    }

    /**
     * 사용자 ID와 역할을 기반으로 JWT 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @param role 사용자 역할
     * @return 생성된 JWT 토큰 문자열
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
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
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * JWT 토큰에서 사용자 역할을 추출한다.
     *
     * @param token JWT 토큰 문자열
     * @return 토큰에 포함된 사용자 역할
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "USER";
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or null: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰의 만료 여부를 확인한다.
     *
     * @param token 확인할 JWT 토큰 문자열
     * @return 토큰이 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("Token is expired: {}", e.getMessage());
            return true;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid token while checking expiration: {}", e.getMessage());
            return true;
        }
    }
}
