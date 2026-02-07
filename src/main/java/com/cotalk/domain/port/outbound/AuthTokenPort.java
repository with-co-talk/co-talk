package com.cotalk.domain.port.outbound;

/**
 * 인증 토큰 발급 포트.
 * 애플리케이션 계층에서 토큰 생성만 필요할 때 사용한다.
 * JWT 등 구체 구현은 인프라 어댑터에서 주입한다.
 */
public interface AuthTokenPort {

    /**
     * 사용자 ID로 Access 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @return 발급된 Access 토큰 문자열
     */
    String generateAccessToken(Long userId);
}
