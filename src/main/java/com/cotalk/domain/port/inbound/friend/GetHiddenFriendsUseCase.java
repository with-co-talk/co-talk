package com.cotalk.domain.port.inbound.friend;

import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;

import java.util.List;

/**
 * 숨긴 친구 목록 조회 유스케이스.
 * 사용자가 숨긴 친구 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetHiddenFriendsUseCase {

    /**
     * 사용자가 숨긴 친구 목록을 조회한다.
     *
     * @param userId 숨긴 친구 목록을 조회할 사용자 ID
     * @return 숨긴 친구 정보 목록
     */
    List<HiddenFriendDto> getHiddenFriends(Long userId);
}
