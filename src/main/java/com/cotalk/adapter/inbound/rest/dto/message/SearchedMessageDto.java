package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;

/**
 * 검색된 메시지 정보 DTO.
 *
 * @param id         메시지 ID
 * @param chatRoomId 채팅방 ID
 * @param senderId   발신자 ID
 * @param content    메시지 내용
 * @param type       메시지 타입
 * @param createdAt  생성 일시
 * @author seunggu.lee
 */
public record SearchedMessageDto(
        Long id,
        Long chatRoomId,
        Long senderId,
        String content,
        String type,
        LocalDateTime createdAt
) {

    /**
     * Message 엔티티로부터 검색된 메시지 DTO를 생성합니다.
     *
     * @param message Message 엔티티
     * @return SearchedMessageDto 인스턴스
     */
    public static SearchedMessageDto from(Message message) {
        return new SearchedMessageDto(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt()
        );
    }
}
