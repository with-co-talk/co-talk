package com.cotalk.adapter.inbound.rest.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 숨긴 친구 목록 응답 DTO.
 * 숨긴 친구 목록을 클라이언트에 전달하기 위한 래퍼 클래스이다.
 *
 * @author seunggu.lee
 */
@Getter
@Builder
@AllArgsConstructor
public class HiddenFriendsResponse {

    /**
     * 숨긴 친구 목록
     */
    private List<HiddenFriendDto> friends;

    /**
     * 숨긴 친구 목록 응답을 생성한다.
     *
     * @param friends 숨긴 친구 DTO 목록
     * @return HiddenFriendsResponse 인스턴스
     */
    public static HiddenFriendsResponse of(List<HiddenFriendDto> friends) {
        return HiddenFriendsResponse.builder()
                .friends(friends)
                .build();
    }
}
