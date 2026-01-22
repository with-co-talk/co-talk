package com.cotalk.domain.port.inbound.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;

import java.util.List;

/**
 * 채팅방 멤버 목록 조회 유스케이스.
 *
 * @author seunggu.lee
 */
public interface GetChatRoomMembersUseCase {

    /**
     * 채팅방의 멤버 목록을 조회한다.
     *
     * @param chatRoomId    채팅방 ID
     * @param requestUserId 요청 사용자 ID (채팅방 멤버여야 함)
     * @return 멤버 정보 목록
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    List<MemberInfo> getChatRoomMembers(Long chatRoomId, Long requestUserId);

    /**
     * 채팅방 멤버 정보 레코드.
     *
     * @param userId    사용자 ID
     * @param nickname  닉네임
     * @param avatarUrl 프로필 이미지 URL
     * @param role      채팅방 내 역할
     */
    record MemberInfo(
            Long userId,
            String nickname,
            String avatarUrl,
            ChatRoomMember.MemberRole role
    ) {}
}
