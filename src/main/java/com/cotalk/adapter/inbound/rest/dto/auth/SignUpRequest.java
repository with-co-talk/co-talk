package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 요청 DTO.
 *
 * @param email    이메일 주소
 * @param password 비밀번호
 * @param nickname 닉네임
 * @author seunggu.lee
 */
public record SignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}
