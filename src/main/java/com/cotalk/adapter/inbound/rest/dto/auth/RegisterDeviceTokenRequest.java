package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 디바이스 토큰 등록 요청 DTO.
 *
 * @param token      디바이스 토큰 (FCM/APNs)
 * @param deviceType 디바이스 타입 (ANDROID, IOS, WEB)
 * @author seunggu.lee
 */
public record RegisterDeviceTokenRequest(
        @NotBlank(message = "디바이스 토큰은 필수입니다.")
        String token,

        @NotBlank(message = "디바이스 타입은 필수입니다. (ANDROID, IOS, WEB)")
        String deviceType
) {}
