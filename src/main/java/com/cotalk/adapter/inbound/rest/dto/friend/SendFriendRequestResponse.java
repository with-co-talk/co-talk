package com.cotalk.adapter.inbound.rest.dto.friend;

/**
 * 친구 요청 전송 응답 DTO.
 *
 * @param requestId 생성된 친구 요청 ID
 * @param message   결과 메시지
 * @author seunggu.lee
 */
public record SendFriendRequestResponse(Long requestId, String message) {

    /**
     * 응답 객체를 생성한다.
     *
     * @param requestId 생성된 친구 요청 ID
     * @param message   결과 메시지
     * @return SendFriendRequestResponse 인스턴스
     */
    public static SendFriendRequestResponse of(Long requestId, String message) {
        return new SendFriendRequestResponse(requestId, message);
    }
}
