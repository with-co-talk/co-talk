package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 계정 삭제 요청 DTO.
 *
 * @param password 확인용 비밀번호
 * @author seunggu.lee
 */
public record DeleteAccountRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
