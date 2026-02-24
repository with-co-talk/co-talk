package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.domain.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 코드를 이용한 비밀번호 재설정 요청 DTO.
 *
 * @param email 이메일 주소
 * @param code 6자리 인증 코드
 * @param newPassword 새 비밀번호
 */
public record ResetPasswordWithCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증 코드는 필수입니다.")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
        String code,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @StrongPassword
        String newPassword
) {}
