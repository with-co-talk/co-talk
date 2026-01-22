package com.cotalk.adapter.inbound.rest.dto.chatroom;

import java.util.List;

/**
 * 채팅방 멤버 목록 응답 DTO.
 *
 * @param members 멤버 목록
 * @author seunggu.lee
 */
public record ChatRoomMembersResponse(
        List<ChatRoomMemberDto> members
) {
    /**
     * 멤버 목록으로부터 응답 DTO를 생성한다.
     *
     * @param members 멤버 목록
     * @return 채팅방 멤버 목록 응답
     */
    public static ChatRoomMembersResponse of(List<ChatRoomMemberDto> members) {
        return new ChatRoomMembersResponse(members);
    }
}
