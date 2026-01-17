package com.cotalk.adapter.inbound.rest.dto.message;

import java.util.List;

/**
 * 메시지 검색 응답 DTO.
 *
 * @param messages 검색된 메시지 목록
 * @author seunggu.lee
 */
public record MessageSearchResponse(List<SearchedMessageDto> messages) {

    /**
     * 메시지 검색 응답을 생성합니다.
     *
     * @param messages 검색된 메시지 목록
     * @return MessageSearchResponse 인스턴스
     */
    public static MessageSearchResponse of(List<SearchedMessageDto> messages) {
        return new MessageSearchResponse(messages);
    }
}
