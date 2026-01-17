package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.Message;

import java.util.List;

/**
 * 메시지 검색 유스케이스.
 * 채팅방 내 또는 전체 채팅방에서 키워드로 메시지를 검색한다.
 *
 * @author seunggu.lee
 */
public interface SearchMessageUseCase {

    /**
     * 특정 채팅방 내에서 키워드로 메시지를 검색한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 검색하는 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     */
    List<Message> searchInChatRoom(Long chatRoomId, Long userId, String keyword, int page, int size);

    /**
     * 사용자가 속한 모든 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     */
    List<Message> searchAcrossAllChatRooms(Long userId, String keyword, int page, int size);
}
