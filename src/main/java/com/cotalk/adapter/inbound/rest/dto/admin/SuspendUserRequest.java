package com.cotalk.adapter.inbound.rest.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 정지 요청 DTO.
 *
 * @param adminId 관리자 ID
 * @param reason  정지 사유
 * @author seunggu.lee
 */
public record SuspendUserRequest(
        @NotNull(message = "관리자 ID는 필수입니다.")
        Long adminId,

        @NotBlank(message = "정지 사유는 필수입니다.")
        String reason
) {}
