package com.cotalk.domain.port.inbound.friend;

import com.cotalk.domain.entity.User;

import java.util.List;

/**
 * 차단 사용자 목록 조회 유스케이스.
 * 사용자가 차단한 사용자 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetBlockedUsersUseCase {

    /**
     * 사용자가 차단한 사용자 목록을 조회한다.
     *
     * @param blockerId 차단한 사용자 ID
     * @return 차단된 사용자 목록
     */
    List<User> getBlockedUsers(Long blockerId);
}
