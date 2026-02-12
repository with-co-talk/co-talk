package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 아이디(이메일) 찾기 응답 DTO.
 *
 * @param found 이메일 찾기 성공 여부
 * @param maskedEmail 마스킹된 이메일
 * @param message 결과 메시지
 */
public record FindEmailResponse(
        boolean found,
        String maskedEmail,
        String message
) {
    public static FindEmailResponse from(boolean found, String maskedEmail, String message) {
        return new FindEmailResponse(found, maskedEmail, message);
    }
}
