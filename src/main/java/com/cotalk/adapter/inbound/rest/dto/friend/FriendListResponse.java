package com.cotalk.adapter.inbound.rest.dto.friend;

import java.util.List;

/**
 * 친구 목록 응답 DTO.
 *
 * @param friends 친구 목록
 * @author seunggu.lee
 */
public record FriendListResponse(List<FriendDto> friends) {

    /**
     * 친구 목록 응답을 생성한다.
     *
     * @param friends 친구 DTO 목록
     * @return FriendListResponse 인스턴스
     */
    public static FriendListResponse of(List<FriendDto> friends) {
        return new FriendListResponse(friends);
    }
}
