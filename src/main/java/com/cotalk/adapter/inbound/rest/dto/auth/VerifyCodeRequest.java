package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 코드 검증 요청 DTO.
 *
 * @param email 이메일 주소
 * @param code 6자리 인증 코드
 */
public record VerifyCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증 코드는 필수입니다.")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
        String code
) {}
