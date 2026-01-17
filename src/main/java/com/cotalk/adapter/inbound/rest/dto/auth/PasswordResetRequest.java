package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 요청 DTO.
 *
 * @param email 비밀번호를 재설정할 이메일 주소
 * @author seunggu.lee
 */
public record PasswordResetRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {}
