package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.domain.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * @param email    이메일 주소
 * @param password 비밀번호 (8-128자, 대문자/소문자/숫자/특수문자 각 1개 이상)
 * @param nickname 닉네임 (2-50자)
 * @param phoneNumber 전화번호 (선택, 20자 이내)
 * @author seunggu.lee
 */
public record SignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @StrongPassword
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2-50자여야 합니다.")
        String nickname,

        @Size(max = 20, message = "전화번호는 20자 이내여야 합니다.")
        String phoneNumber
) {}
