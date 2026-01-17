package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 메시지 답장 요청 DTO.
 *
 * @param senderId 발신자 ID
 * @param content  답장 내용
 * @author seunggu.lee
 */
public record ReplyMessageRequest(
        @NotNull(message = "발신자 ID는 필수입니다.")
        Long senderId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {

    /**
     * 메시지 답장 요청을 생성합니다.
     *
     * @param senderId 발신자 ID
     * @param content  답장 내용
     * @return ReplyMessageRequest 인스턴스
     */
    public static ReplyMessageRequest of(Long senderId, String content) {
        return new ReplyMessageRequest(senderId, content);
    }
}
