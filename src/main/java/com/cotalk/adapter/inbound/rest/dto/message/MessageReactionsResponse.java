package com.cotalk.adapter.inbound.rest.dto.message;

import java.util.List;

/**
 * 메시지 반응 목록 응답 DTO.
 *
 * @param reactions 반응 목록
 * @author seunggu.lee
 */
public record MessageReactionsResponse(List<MessageReactionResponse> reactions) {

    /**
     * 메시지 반응 목록 응답을 생성합니다.
     *
     * @param reactions 반응 목록
     * @return MessageReactionsResponse 인스턴스
     */
    public static MessageReactionsResponse of(List<MessageReactionResponse> reactions) {
        return new MessageReactionsResponse(reactions);
    }
}
