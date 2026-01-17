package com.cotalk.domain.port.inbound.chatroom;

import java.util.List;

/**
 * 그룹 채팅방 멤버 초대 유스케이스.
 * 그룹 채팅방에 새로운 멤버를 초대한다.
 *
 * @author seunggu.lee
 */
public interface InviteGroupChatMemberUseCase {

    /**
     * 그룹 채팅방에 멤버를 초대한다.
     *
     * @param roomId 채팅방 ID
     * @param inviterId 초대하는 사용자 ID
     * @param inviteeIds 초대받는 사용자 ID 목록
     */
    void inviteMembers(Long roomId, Long inviterId, List<Long> inviteeIds);
}
