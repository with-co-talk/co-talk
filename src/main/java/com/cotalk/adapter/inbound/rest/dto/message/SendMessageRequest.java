package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 메시지 전송 요청 DTO.
 *
 * @param chatRoomId 채팅방 ID
 * @param content    메시지 내용 (최대 5000자)
 * @author seunggu.lee
 */
public record SendMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 5000, message = "메시지는 최대 5000자까지 가능합니다.")
        String content
) {

    /**
     * 메시지 전송 요청을 생성합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param content    메시지 내용
     * @return SendMessageRequest 인스턴스
     */
    public static SendMessageRequest of(Long chatRoomId, String content) {
        return new SendMessageRequest(chatRoomId, content);
    }
}
