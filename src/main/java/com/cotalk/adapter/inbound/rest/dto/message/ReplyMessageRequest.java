package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;

/**
 * 메시지 답장 요청 DTO.
 *
 * @param content 답장 내용
 * @author seunggu.lee
 */
public record ReplyMessageRequest(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {

    /**
     * 메시지 답장 요청을 생성합니다.
     *
     * @param content 답장 내용
     * @return ReplyMessageRequest 인스턴스
     */
    public static ReplyMessageRequest of(String content) {
        return new ReplyMessageRequest(content);
    }
}
