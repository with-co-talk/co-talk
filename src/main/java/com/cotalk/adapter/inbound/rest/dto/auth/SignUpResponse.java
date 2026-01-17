package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 회원가입 응답 DTO.
 *
 * @param userId  생성된 사용자 ID
 * @param message 응답 메시지
 * @author seunggu.lee
 */
public record SignUpResponse(Long userId, String message) {

    /**
     * SignUpResponse를 생성한다.
     *
     * @param userId  생성된 사용자 ID
     * @param message 응답 메시지
     * @return SignUpResponse 인스턴스
     */
    public static SignUpResponse of(Long userId, String message) {
        return new SignUpResponse(userId, message);
    }
}
