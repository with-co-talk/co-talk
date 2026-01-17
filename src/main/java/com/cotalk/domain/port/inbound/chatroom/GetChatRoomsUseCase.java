package com.cotalk.domain.port.inbound.chatroom;

import com.cotalk.domain.entity.ChatRoomSummary;

import java.util.List;

/**
 * 채팅방 목록 조회 유스케이스.
 * 사용자가 참여 중인 채팅방 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetChatRoomsUseCase {

    /**
     * 사용자가 참여 중인 채팅방 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 요약 정보 목록
     */
    List<ChatRoomSummary> getChatRooms(Long userId);
}
