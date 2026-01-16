package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.Message;

import java.util.List;


public interface GetMessageHistoryUseCase {
    /**
     * 커서 기반으로 메시지 히스토리를 조회합니다.
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 조회 (null이면 최신 메시지부터)
     * @param size 조회할 메시지 개수
     * @return 메시지 목록 (최신순 정렬)
     */
    List<Message> getMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size);
}
