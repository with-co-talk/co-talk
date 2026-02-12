package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.infrastructure.security.PasswordValidator;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO.
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새 비밀번호 (8-128자, 대문자/소문자/숫자/특수문자 각 1개 이상)
 * @author seunggu.lee
 */
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @PasswordValidator
        String newPassword
) {}
