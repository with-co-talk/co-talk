package com.cotalk.adapter.inbound.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 정지 요청 DTO.
 *
 * @param reason 정지 사유
 * @author seunggu.lee
 */
public record SuspendUserRequest(
        @NotBlank(message = "정지 사유는 필수입니다.")
        String reason
) {}
