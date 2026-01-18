package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;

/**
 * 반응 제거 요청 DTO.
 *
 * @param emoji 제거할 이모지
 * @author seunggu.lee
 */
public record RemoveReactionRequest(
        @NotBlank(message = "이모지는 필수입니다.")
        String emoji
) {

    /**
     * 반응 제거 요청을 생성합니다.
     *
     * @param emoji 제거할 이모지
     * @return RemoveReactionRequest 인스턴스
     */
    public static RemoveReactionRequest of(String emoji) {
        return new RemoveReactionRequest(emoji);
    }
}
