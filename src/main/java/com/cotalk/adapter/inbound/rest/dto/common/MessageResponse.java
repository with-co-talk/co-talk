package com.cotalk.adapter.inbound.rest.dto.common;

/**
 * 일반 메시지 응답 DTO.
 * 단순 성공/실패 메시지를 반환할 때 사용한다.
 *
 * @param message 결과 메시지
 * @author seunggu.lee
 */
public record MessageResponse(String message) {

    /**
     * 성공 메시지 응답을 생성한다.
     *
     * @param message 성공 메시지
     * @return MessageResponse 인스턴스
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
