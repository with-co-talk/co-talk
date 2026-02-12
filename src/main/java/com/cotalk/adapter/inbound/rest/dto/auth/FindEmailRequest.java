package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 아이디(이메일) 찾기 요청 DTO.
 *
 * @param nickname 닉네임
 * @param phoneNumber 전화번호
 */
public record FindEmailRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @NotBlank(message = "전화번호는 필수입니다.")
        String phoneNumber
) {}
