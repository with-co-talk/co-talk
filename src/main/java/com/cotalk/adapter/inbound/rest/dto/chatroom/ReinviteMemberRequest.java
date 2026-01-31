package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotNull;

/**
 * 1:1 채팅방 멤버 재초대 요청 DTO.
 *
 * @param inviteeId 재초대할 멤버 ID
 * @author seunggu.lee
 */
public record ReinviteMemberRequest(
        @NotNull(message = "재초대할 멤버 ID는 필수입니다.")
        Long inviteeId
) {

    /**
     * 멤버 재초대 요청을 생성합니다.
     *
     * @param inviteeId 재초대할 멤버 ID
     * @return ReinviteMemberRequest 인스턴스
     */
    public static ReinviteMemberRequest of(Long inviteeId) {
        return new ReinviteMemberRequest(inviteeId);
    }
}
