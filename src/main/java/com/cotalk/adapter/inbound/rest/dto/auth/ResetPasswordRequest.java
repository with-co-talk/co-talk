package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 DTO.
 *
 * @param token       비밀번호 재설정 토큰
 * @param newPassword 새 비밀번호
 * @author seunggu.lee
 */
public record ResetPasswordRequest(
        @NotBlank(message = "토큰은 필수입니다.")
        String token,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        String newPassword
) {}
