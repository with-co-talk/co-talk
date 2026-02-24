package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.domain.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 DTO.
 *
 * @param token       비밀번호 재설정 토큰
 * @param newPassword 새 비밀번호 (8-128자, 대문자/소문자/숫자/특수문자 각 1개 이상)
 * @author seunggu.lee
 */
public record ResetPasswordRequest(
        @NotBlank(message = "토큰은 필수입니다.")
        String token,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @StrongPassword
        String newPassword
) {}
