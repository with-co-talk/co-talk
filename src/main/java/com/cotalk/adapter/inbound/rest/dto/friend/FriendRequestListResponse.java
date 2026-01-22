package com.cotalk.adapter.inbound.rest.dto.friend;

import java.util.List;

/**
 * 친구 요청 목록 응답 DTO.
 *
 * @param requests 친구 요청 목록
 * @author seunggu.lee
 */
public record FriendRequestListResponse(List<FriendRequestDto> requests) {

    /**
     * 친구 요청 목록 응답을 생성한다.
     *
     * @param requests 친구 요청 DTO 목록
     * @return FriendRequestListResponse 인스턴스
     */
    public static FriendRequestListResponse of(List<FriendRequestDto> requests) {
        return new FriendRequestListResponse(requests);
    }
}
