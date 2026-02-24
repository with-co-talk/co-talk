package com.cotalk.domain.port.inbound.auth;

/**
 * 로그인 결과.
 * 로그인 성공 시 Access Token, 사용자 ID, 토큰 만료 시간을 함께 반환한다.
 *
 * @param accessToken 발급된 JWT Access Token
 * @param userId 사용자 ID
 * @param expiresInSeconds Access Token 만료 시간 (초)
 * @author seunggu.lee
 */
public record LoginResult(String accessToken, Long userId, long expiresInSeconds) {
}
