package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 멤버 초대 요청 DTO.
 *
 * @param inviteeIds 초대할 멤버 ID 목록
 * @author seunggu.lee
 */
public record InviteMembersRequest(
        @NotNull(message = "초대할 멤버 목록은 필수입니다.")
        List<Long> inviteeIds
) {

    /**
     * 멤버 초대 요청을 생성합니다.
     *
     * @param inviteeIds 초대할 멤버 ID 목록
     * @return InviteMembersRequest 인스턴스
     */
    public static InviteMembersRequest of(List<Long> inviteeIds) {
        return new InviteMembersRequest(inviteeIds);
    }
}
