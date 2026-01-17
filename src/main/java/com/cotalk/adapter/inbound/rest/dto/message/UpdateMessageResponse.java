package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;

/**
 * 메시지 수정 응답 DTO.
 *
 * @param messageId 메시지 ID
 * @param content   수정된 내용
 * @param updatedAt 수정 일시
 * @author seunggu.lee
 */
public record UpdateMessageResponse(
        Long messageId,
        String content,
        LocalDateTime updatedAt
) {

    /**
     * Message 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param message Message 엔티티
     * @return UpdateMessageResponse 인스턴스
     */
    public static UpdateMessageResponse from(Message message) {
        return new UpdateMessageResponse(
                message.getId(),
                message.getContent(),
                message.getUpdatedAt()
        );
    }
}
