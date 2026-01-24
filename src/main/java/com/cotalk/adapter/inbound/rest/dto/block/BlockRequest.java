package com.cotalk.adapter.inbound.rest.dto.block;

import jakarta.validation.constraints.NotNull;

/**
 * 차단 요청 DTO.
 * 차단하는 사용자는 인증 정보에서 자동으로 추출됩니다.
 *
 * @param blockedId 차단할 사용자 ID
 * @author seunggu.lee
 */
public record BlockRequest(
        @NotNull(message = "차단할 사용자 ID는 필수입니다.")
        Long blockedId
) {}
