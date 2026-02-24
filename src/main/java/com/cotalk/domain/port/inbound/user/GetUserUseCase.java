package com.cotalk.domain.port.inbound.user;

import com.cotalk.domain.entity.User;

import java.util.List;

/**
 * 사용자 조회 유스케이스.
 * ID로 단일 또는 다수의 사용자를 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetUserUseCase {

    /**
     * ID로 사용자를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자
     * @throws com.cotalk.domain.exception.UserNotFoundException 사용자가 존재하지 않는 경우
     */
    User getUserById(Long userId);

    /**
     * 여러 ID로 사용자 목록을 조회한다.
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 조회된 사용자 목록
     */
    List<User> getUsersByIds(List<Long> userIds);
}
