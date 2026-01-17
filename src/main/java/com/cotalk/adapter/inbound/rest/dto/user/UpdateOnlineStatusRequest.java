package com.cotalk.adapter.inbound.rest.dto.user;

import com.cotalk.domain.entity.User.OnlineStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 온라인 상태 업데이트 요청 DTO.
 *
 * @param status 변경할 온라인 상태
 * @author seunggu.lee
 */
public record UpdateOnlineStatusRequest(
        @NotNull(message = "온라인 상태는 필수입니다.")
        OnlineStatus status
) {
}
