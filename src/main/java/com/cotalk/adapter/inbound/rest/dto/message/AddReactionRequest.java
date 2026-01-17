package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 반응 추가 요청 DTO.
 *
 * @param userId 사용자 ID
 * @param emoji  이모지
 * @author seunggu.lee
 */
public record AddReactionRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotBlank(message = "이모지는 필수입니다.")
        String emoji
) {

    /**
     * 반응 추가 요청을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param emoji  이모지
     * @return AddReactionRequest 인스턴스
     */
    public static AddReactionRequest of(Long userId, String emoji) {
        return new AddReactionRequest(userId, emoji);
    }
}
