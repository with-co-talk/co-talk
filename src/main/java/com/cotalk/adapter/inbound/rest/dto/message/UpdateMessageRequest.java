package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 메시지 수정 요청 DTO.
 *
 * @param userId  요청자 ID
 * @param content 새 메시지 내용
 * @author seunggu.lee
 */
public record UpdateMessageRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {

    /**
     * 메시지 수정 요청을 생성합니다.
     *
     * @param userId  요청자 ID
     * @param content 새 메시지 내용
     * @return UpdateMessageRequest 인스턴스
     */
    public static UpdateMessageRequest of(Long userId, String content) {
        return new UpdateMessageRequest(userId, content);
    }
}
