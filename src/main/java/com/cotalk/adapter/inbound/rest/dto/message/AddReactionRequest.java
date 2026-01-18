package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;

/**
 * 반응 추가 요청 DTO.
 *
 * @param emoji 이모지
 * @author seunggu.lee
 */
public record AddReactionRequest(
        @NotBlank(message = "이모지는 필수입니다.")
        String emoji
) {

    /**
     * 반응 추가 요청을 생성합니다.
     *
     * @param emoji 이모지
     * @return AddReactionRequest 인스턴스
     */
    public static AddReactionRequest of(String emoji) {
        return new AddReactionRequest(emoji);
    }
}
