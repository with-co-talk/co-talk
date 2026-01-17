package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 토큰 검증 응답 DTO.
 *
 * @param valid 토큰 유효성 여부
 * @author seunggu.lee
 */
public record TokenValidationResponse(boolean valid) {

    /**
     * TokenValidationResponse를 생성한다.
     *
     * @param valid 토큰 유효성 여부
     * @return TokenValidationResponse 인스턴스
     */
    public static TokenValidationResponse of(boolean valid) {
        return new TokenValidationResponse(valid);
    }
}
