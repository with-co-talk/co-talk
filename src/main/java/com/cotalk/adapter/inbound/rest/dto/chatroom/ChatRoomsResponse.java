package com.cotalk.adapter.inbound.rest.dto.chatroom;

import com.cotalk.domain.model.PageResult;

import java.util.List;

/**
 * 채팅방 목록 응답 DTO.
 * 페이지네이션 메타데이터를 포함한다.
 *
 * @param rooms         채팅방 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @author seunggu.lee
 */
public record ChatRoomsResponse(
        List<ChatRoomDto> rooms,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 채팅방 목록 응답을 생성합니다. (하위 호환용)
     *
     * @param rooms 채팅방 목록
     * @return ChatRoomsResponse 인스턴스
     */
    public static ChatRoomsResponse of(List<ChatRoomDto> rooms) {
        return new ChatRoomsResponse(rooms, 0, rooms.size(), rooms.size(), 1);
    }

    /**
     * PageResult 객체와 매핑된 DTO 목록으로부터 응답을 생성합니다.
     *
     * @param rooms    채팅방 DTO 목록
     * @param pageData PageResult 메타데이터 소스
     * @return ChatRoomsResponse 인스턴스
     */
    public static ChatRoomsResponse of(List<ChatRoomDto> rooms, PageResult<?> pageData) {
        return new ChatRoomsResponse(
                rooms,
                pageData.page(),
                pageData.size(),
                pageData.totalElements(),
                pageData.totalPages()
        );
    }
}
