package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 로그인 응답 DTO.
 *
 * @param token     JWT 토큰
 * @param tokenType 토큰 타입 (Bearer)
 * @author seunggu.lee
 */
public record LoginResponse(String token, String tokenType) {

    /**
     * Bearer 타입의 LoginResponse를 생성한다.
     *
     * @param token JWT 토큰
     * @return LoginResponse 인스턴스
     */
    public static LoginResponse of(String token) {
        return new LoginResponse(token, "Bearer");
    }

    /**
     * LoginResponse를 생성한다.
     *
     * @param token     JWT 토큰
     * @param tokenType 토큰 타입
     * @return LoginResponse 인스턴스
     */
    public static LoginResponse of(String token, String tokenType) {
        return new LoginResponse(token, tokenType);
    }
}
