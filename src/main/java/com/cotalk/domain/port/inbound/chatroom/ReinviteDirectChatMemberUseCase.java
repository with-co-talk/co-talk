package com.cotalk.domain.port.inbound.chatroom;

/**
 * 1:1 채팅방 멤버 재초대 유스케이스.
 * 1:1 채팅방에서 나간 상대방을 다시 초대한다.
 *
 * @author seunggu.lee
 */
public interface ReinviteDirectChatMemberUseCase {

    /**
     * 1:1 채팅방에서 나간 상대방을 재초대한다.
     *
     * @param roomId 채팅방 ID
     * @param inviterId 초대하는 사용자 ID (현재 채팅방에 남아있는 사용자)
     * @param inviteeId 재초대할 사용자 ID (나갔던 사용자)
     */
    void reinviteMember(Long roomId, Long inviterId, Long inviteeId);
}
