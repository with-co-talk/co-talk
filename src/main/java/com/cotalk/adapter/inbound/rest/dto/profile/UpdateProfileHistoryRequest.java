package com.cotalk.adapter.inbound.rest.dto.profile;

import jakarta.validation.constraints.NotNull;

/**
 * 프로필 이력 수정 요청 DTO.
 *
 * @param isPrivate 나만보기 여부
 * @author seunggu.lee
 */
public record UpdateProfileHistoryRequest(
        @NotNull(message = "나만보기 여부는 필수입니다.")
        Boolean isPrivate
) {
}
