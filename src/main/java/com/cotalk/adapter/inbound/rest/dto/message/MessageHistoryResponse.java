package com.cotalk.adapter.inbound.rest.dto.message;

import java.util.List;

/**
 * 메시지 히스토리 응답 DTO.
 *
 * @param messages   메시지 목록
 * @param nextCursor 다음 페이지 커서 (마지막 메시지 ID)
 * @param hasMore    다음 페이지 존재 여부
 * @author seunggu.lee
 */
public record MessageHistoryResponse(List<MessageDto> messages, Long nextCursor, boolean hasMore) {

    /**
     * 메시지 히스토리 응답을 생성합니다.
     *
     * @param messages   메시지 목록
     * @param nextCursor 다음 페이지 커서
     * @param hasMore    다음 페이지 존재 여부
     * @return MessageHistoryResponse 인스턴스
     */
    public static MessageHistoryResponse of(List<MessageDto> messages, Long nextCursor, boolean hasMore) {
        return new MessageHistoryResponse(messages, nextCursor, hasMore);
    }
}
