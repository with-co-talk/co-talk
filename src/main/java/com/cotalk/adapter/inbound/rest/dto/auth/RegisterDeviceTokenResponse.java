package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 디바이스 토큰 등록 응답 DTO.
 *
 * @param tokenId 등록된 토큰 ID
 * @param message 응답 메시지
 * @author seunggu.lee
 */
public record RegisterDeviceTokenResponse(Long tokenId, String message) {

    /**
     * RegisterDeviceTokenResponse를 생성한다.
     *
     * @param tokenId 등록된 토큰 ID
     * @param message 응답 메시지
     * @return RegisterDeviceTokenResponse 인스턴스
     */
    public static RegisterDeviceTokenResponse of(Long tokenId, String message) {
        return new RegisterDeviceTokenResponse(tokenId, message);
    }
}
