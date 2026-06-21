package com.cotalk.domain.port.inbound.chatroom;

import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;

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

    /**
     * 사용자가 참여 중인 채팅방 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 채팅방 요약 정보
     */
    PageResult<ChatRoomSummary> getChatRooms(Long userId, PageQuery query);
}
