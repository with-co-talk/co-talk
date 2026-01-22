package com.cotalk.domain.port.inbound.chatroom;

/**
 * 채팅방 멤버 강제 퇴장 유스케이스.
 *
 * @author seunggu.lee
 */
public interface KickChatRoomMemberUseCase {

    /**
     * 채팅방에서 멤버를 강제 퇴장시킨다.
     * 관리자만 이 기능을 사용할 수 있다.
     *
     * @param chatRoomId   채팅방 ID
     * @param adminUserId  관리자 사용자 ID
     * @param targetUserId 강제 퇴장시킬 사용자 ID
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 관리자가 아니거나 대상이 채팅방 멤버가 아닌 경우
     * @throws com.cotalk.domain.exception.InvalidChatRoomException      1:1 채팅방이거나 자기 자신을 퇴장시키려는 경우
     */
    void kickMember(Long chatRoomId, Long adminUserId, Long targetUserId);
}
