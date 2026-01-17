package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 메시지 전송 요청 DTO.
 *
 * @param senderId   발신자 ID
 * @param chatRoomId 채팅방 ID
 * @param content    메시지 내용
 * @author seunggu.lee
 */
public record SendMessageRequest(
        @NotNull(message = "발신자 ID는 필수입니다.")
        Long senderId,

        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {

    /**
     * 메시지 전송 요청을 생성합니다.
     *
     * @param senderId   발신자 ID
     * @param chatRoomId 채팅방 ID
     * @param content    메시지 내용
     * @return SendMessageRequest 인스턴스
     */
    public static SendMessageRequest of(Long senderId, Long chatRoomId, String content) {
        return new SendMessageRequest(senderId, chatRoomId, content);
    }
}
