package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 *
 * @param email    이메일 주소
 * @param password 비밀번호
 * @author seunggu.lee
 */
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
