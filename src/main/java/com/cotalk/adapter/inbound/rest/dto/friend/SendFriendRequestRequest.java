package com.cotalk.adapter.inbound.rest.dto.friend;

import jakarta.validation.constraints.NotNull;

/**
 * 친구 요청 전송 요청 DTO.
 *
 * @param requesterId 요청자 ID
 * @param receiverId  수신자 ID
 * @author seunggu.lee
 */
public record SendFriendRequestRequest(
        @NotNull(message = "요청자 ID는 필수입니다.")
        Long requesterId,

        @NotNull(message = "수신자 ID는 필수입니다.")
        Long receiverId
) {}
