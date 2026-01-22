package com.cotalk.adapter.inbound.rest.dto.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;

/**
 * 채팅방 멤버 정보 DTO.
 *
 * @param userId    사용자 ID
 * @param nickname  닉네임
 * @param avatarUrl 프로필 이미지 URL
 * @param role      채팅방 내 역할
 * @author seunggu.lee
 */
public record ChatRoomMemberDto(
        Long userId,
        String nickname,
        String avatarUrl,
        ChatRoomMember.MemberRole role
) {
    /**
     * MemberInfo로부터 DTO를 생성한다.
     *
     * @param memberInfo 멤버 정보
     * @return 채팅방 멤버 DTO
     */
    public static ChatRoomMemberDto from(GetChatRoomMembersUseCase.MemberInfo memberInfo) {
        return new ChatRoomMemberDto(
                memberInfo.userId(),
                memberInfo.nickname(),
                memberInfo.avatarUrl(),
                memberInfo.role()
        );
    }
}
