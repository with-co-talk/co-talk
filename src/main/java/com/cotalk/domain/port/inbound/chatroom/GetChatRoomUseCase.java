package com.cotalk.domain.port.inbound.chatroom;

import com.cotalk.domain.entity.ChatRoomSummary;

/**
 * 단일 채팅방 조회 유스케이스.
 * 특정 채팅방의 상세 정보를 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetChatRoomUseCase {

    /**
     * 특정 채팅방의 상세 정보를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 조회하는 사용자 ID
     * @return 채팅방 요약 정보
     */
    ChatRoomSummary getChatRoom(Long roomId, Long userId);
}
