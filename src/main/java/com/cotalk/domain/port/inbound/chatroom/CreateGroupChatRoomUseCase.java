package com.cotalk.domain.port.inbound.chatroom;

import java.util.List;

/**
 * 그룹 채팅방 생성 유스케이스.
 * 여러 사용자가 참여하는 그룹 채팅방을 생성한다.
 *
 * @author seunggu.lee
 */
public interface CreateGroupChatRoomUseCase {

    /**
     * 그룹 채팅방을 생성한다.
     *
     * @param creatorId 생성자 ID
     * @param roomName 채팅방 이름
     * @param memberIds 초대할 멤버 ID 목록 (생성자 제외)
     * @return 생성된 채팅방 ID
     */
    Long createGroupChatRoom(Long creatorId, String roomName, List<Long> memberIds);
}
